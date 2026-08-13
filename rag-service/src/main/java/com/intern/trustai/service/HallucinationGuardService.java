package com.intern.trustai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intern.trustai.entity.ChatMessage;
import com.intern.trustai.entity.Document;
import com.intern.trustai.entity.HallucinationCheck;
import com.intern.trustai.repository.ChatMessageRepository;
import com.intern.trustai.repository.DocumentRepository;
import com.intern.trustai.repository.HallucinationCheckRepository;
import com.intern.trustai.security.TenantContext;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
public class HallucinationGuardService {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final HallucinationCheckRepository checkRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;

    @Value("${trustai.hallucination.threshold.verified:0.80}")
    private double verifiedThreshold;

    @Value("${trustai.hallucination.threshold.uncertain:0.60}")
    private double uncertainThreshold;

    public HallucinationGuardService(ChatLanguageModel chatLanguageModel,
                                     EmbeddingModel embeddingModel,
                                     EmbeddingStore<TextSegment> embeddingStore,
                                     HallucinationCheckRepository checkRepository,
                                     ChatMessageRepository chatMessageRepository,
                                     DocumentRepository documentRepository) {
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.checkRepository = checkRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.documentRepository = documentRepository;
        this.objectMapper = new ObjectMapper();
    }

    public GuardResult verifyClaims(String aiResponse, Long messageId, String tenantId) {
        String promptString = "You are a strict compliance auditor. Your job is to extract all factual claims from the AI Response.\n" +
                "You MUST output ONLY a valid JSON object in the exact format below, with no markdown formatting or extra text:\n" +
                "{\n" +
                "  \"claims\": [\n" +
                "    { \"text\": \"claim 1\" },\n" +
                "    { \"text\": \"claim 2\" }\n" +
                "  ]\n" +
                "}\n\n" +
                "AI Response:\n{{response}}";

        PromptTemplate promptTemplate = PromptTemplate.from(promptString);
        Map<String, Object> variables = new HashMap<>();
        variables.put("response", aiResponse);

        String jsonResult = chatLanguageModel.generate(promptTemplate.apply(variables).text());
        
        // Clean up markdown if LLM adds ```json
        if (jsonResult.startsWith("```json")) {
            jsonResult = jsonResult.substring(7);
        } else if (jsonResult.startsWith("```")) {
            jsonResult = jsonResult.substring(3);
        }
        if (jsonResult.endsWith("```")) {
            jsonResult = jsonResult.substring(0, jsonResult.length() - 3);
        }
        jsonResult = jsonResult.trim();

        GuardResult result = new GuardResult();
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResult);
            JsonNode claimsNode = rootNode.get("claims");
            
            ChatMessage chatMessage = chatMessageRepository.findById(messageId).orElse(null);
            
            int totalClaims = 0;
            double sumScores = 0.0;
            
            ArrayNode claimsArray = objectMapper.createArrayNode();

            if (claimsNode != null && claimsNode.isArray()) {
                for (JsonNode claimNode : claimsNode) {
                    totalClaims++;
                    String text = claimNode.get("text").asText();
                    
                    // Generate embedding for the claim
                    Embedding claimEmbedding = embeddingModel.embed(text).content();
                    
                    // Search top-3 chunks
                    EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                            .queryEmbedding(claimEmbedding)
                            .maxResults(3)
                            .filter(metadataKey("tenant_id").isEqualTo(tenantId))
                            .build();
                            
                    EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
                    
                    double maxScore = 0.0;
                    String bestDocIdStr = null;
                    List<Map<String, Object>> chunksData = new ArrayList<>();
                    
                    for (var match : searchResult.matches()) {
                        if (match.score() > maxScore) {
                            maxScore = match.score();
                            bestDocIdStr = match.embedded().metadata().getString("document_id");
                        }
                        Map<String, Object> chunkInfo = new HashMap<>();
                        chunkInfo.put("text", match.embedded().text());
                        chunkInfo.put("score", match.score());
                        chunksData.add(chunkInfo);
                    }
                    
                    String status;
                    boolean isSupported = false;
                    if (maxScore >= verifiedThreshold) {
                        status = "VÉRIFIÉ";
                        isSupported = true;
                    } else if (maxScore >= uncertainThreshold) {
                        status = "INCERTAIN";
                    } else {
                        status = "NON VÉRIFIÉ";
                    }
                    
                    sumScores += maxScore;
                    
                    String chunksJson = objectMapper.writeValueAsString(chunksData);
                    
                    if (chatMessage != null) {
                        HallucinationCheck check = new HallucinationCheck();
                        check.setMessage(chatMessage);
                        check.setClaimText(text);
                        check.setStatus(status);
                        check.setSimilarityScore(maxScore);
                        check.setSourceChunks(chunksJson);
                        if (bestDocIdStr != null) {
                            documentRepository.findById(Long.parseLong(bestDocIdStr)).ifPresent(check::setDocument);
                        }
                        checkRepository.save(check);
                    }
                    
                    ObjectNode newClaimNode = objectMapper.createObjectNode();
                    newClaimNode.put("text", text);
                    newClaimNode.put("isSupported", isSupported);
                    newClaimNode.put("status", status);
                    newClaimNode.put("score", maxScore);
                    newClaimNode.set("chunks", objectMapper.valueToTree(chunksData));
                    
                    claimsArray.add(newClaimNode);
                }
            }
            
            int confidenceScore = totalClaims == 0 ? 100 : (int) Math.round((sumScores / totalClaims) * 100);
            
            ObjectNode finalAnalysis = objectMapper.createObjectNode();
            finalAnalysis.put("confidenceScore", confidenceScore);
            finalAnalysis.set("claims", claimsArray);
            
            result.setConfidenceScore(confidenceScore);
            result.setClaimAnalysis(objectMapper.writeValueAsString(finalAnalysis));
            
        } catch (Exception e) {
            e.printStackTrace();
            result.setConfidenceScore(0);
            result.setClaimAnalysis("{\"confidenceScore\":0, \"claims\":[], \"error\":\"Failed to parse guard response\"}");
        }

        return result;
    }

    public static class GuardResult {
        private int confidenceScore;
        private String claimAnalysis;

        public int getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
        public String getClaimAnalysis() { return claimAnalysis; }
        public void setClaimAnalysis(String claimAnalysis) { this.claimAnalysis = claimAnalysis; }
    }
}
