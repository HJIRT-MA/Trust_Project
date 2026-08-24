package com.intern.trustai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
public class SwcRagService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public SwcRagService(EmbeddingStore<TextSegment> embeddingStore,
                         EmbeddingModel embeddingModel,
                         ChatLanguageModel chatLanguageModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void initSwcRegistry() {
        try {
            // Check if already ingested by searching for SWC
            Embedding dummy = embeddingModel.embed("test").content();
            EmbeddingSearchRequest checkReq = EmbeddingSearchRequest.builder()
                    .queryEmbedding(dummy)
                    .maxResults(1)
                    .filter(metadataKey("is_swc").isEqualTo("true"))
                    .build();
            
            if (embeddingStore.search(checkReq).matches().isEmpty()) {
                System.out.println("Ingesting SWC Registry into pgvector...");
                InputStream is = new ClassPathResource("data/swc_registry.json").getInputStream();
                List<Map<String, String>> swcList = objectMapper.readValue(is, new TypeReference<>() {});
                
                for (Map<String, String> swc : swcList) {
                    String content = swc.get("swcId") + " " + swc.get("title") + "\n" + swc.get("description");
                    TextSegment segment = TextSegment.from(content);
                    segment.metadata().put("is_swc", "true");
                    segment.metadata().put("swcId", swc.get("swcId"));
                    segment.metadata().put("title", swc.get("title"));
                    segment.metadata().put("vulnerableExample", swc.get("vulnerableExample"));
                    segment.metadata().put("description", swc.get("description"));

                    Embedding embedding = embeddingModel.embed(segment).content();
                    embeddingStore.add(embedding, segment);
                }
                System.out.println("SWC Registry ingestion complete.");
            }
        } catch (Exception e) {
            System.err.println("Failed to ingest SWC registry: " + e.getMessage());
        }
    }

    public Map<String, String> enrichFinding(String findingTitle, String findingDescription, String functionCode) {
        try {
            String query = findingTitle + " " + findingDescription;
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(1)
                    .filter(metadataKey("is_swc").isEqualTo("true"))
                    .build();

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);
            
            if (!result.matches().isEmpty()) {
                TextSegment bestMatch = result.matches().get(0).embedded();
                String swcId = bestMatch.metadata().getString("swcId");
                String swcTitle = bestMatch.metadata().getString("title");
                String swcDesc = bestMatch.metadata().getString("description");
                String vulnExample = bestMatch.metadata().getString("vulnerableExample");

                String prompt = "You are a smart contract auditor. I have found a vulnerability: '" + findingTitle + "' - " + findingDescription + ".\n" +
                        "Based on the following SWC classification:\n" +
                        "SWC ID: " + swcId + "\n" +
                        "Title: " + swcTitle + "\n" +
                        "Description: " + swcDesc + "\n\n" +
                        "Please provide a concise, enriched explanation (max 3 sentences) explaining why the vulnerability in the user's code matches this SWC.";

                String enrichedExplanation = chatLanguageModel.generate(prompt);

                return Map.of(
                        "swcId", swcId,
                        "swcTitle", swcTitle,
                        "enrichedExplanation", enrichedExplanation,
                        "vulnerableExample", vulnExample
                );
            }
        } catch (Exception e) {
            System.err.println("Error enriching finding: " + e.getMessage());
        }
        return Map.of();
    }
}
