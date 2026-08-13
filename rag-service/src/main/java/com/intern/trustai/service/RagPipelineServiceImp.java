package com.intern.trustai.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.intern.trustai.dto.ChunkResponse;
import com.intern.trustai.dto.DashboardStatsDTO;
import com.intern.trustai.dto.RequestHistoryItem;
import com.intern.trustai.dto.TokenDistributionItem;
import com.intern.trustai.entity.Chunk;
import com.intern.trustai.repository.ChunkRepository;
import com.intern.trustai.repository.DocumentRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import java.util.Map;
import java.util.HashMap;
import org.apache.tika.Tika;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import com.intern.trustai.entity.Conversation;
import com.intern.trustai.entity.ChatMessage;
import com.intern.trustai.repository.ConversationRepository;
import com.intern.trustai.repository.ChatMessageRepository;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import com.intern.trustai.security.TenantContext;


import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
public class RagPipelineServiceImp implements RagPipelineService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Tika tika;
    private final JdbcTemplate jdbcTemplate;
    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final HallucinationGuardService guardService;
    private final KafkaProducerService kafkaProducerService;

    public RagPipelineServiceImp(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore,
                                 DocumentRepository documentRepository, ChunkRepository chunkRepository,
                                 SimpMessagingTemplate messagingTemplate, JdbcTemplate jdbcTemplate,
                                 ChatLanguageModel chatLanguageModel, StreamingChatLanguageModel streamingChatLanguageModel, ConversationRepository conversationRepository,
                                 ChatMessageRepository chatMessageRepository, HallucinationGuardService guardService,
                                 KafkaProducerService kafkaProducerService) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.messagingTemplate = messagingTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.chatLanguageModel = chatLanguageModel;
        this.streamingChatLanguageModel = streamingChatLanguageModel;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.guardService = guardService;
        this.kafkaProducerService = kafkaProducerService;
        this.tika = new Tika();
    }

    @Transactional(rollbackFor = Exception.class)
    public void ingestFile(MultipartFile file) throws Exception {
        // Notifier le début
        sendProgress(0, "Extraction du texte en cours...");

        // 1. Sauvegarder le Document en base (JPA)
        com.intern.trustai.entity.Document dbDoc = new com.intern.trustai.entity.Document();
        dbDoc.setFilename(file.getOriginalFilename());
        dbDoc.setContentType(file.getContentType());
        dbDoc.setFileSize(file.getSize());
        dbDoc.setUploadedAt(LocalDateTime.now());
        dbDoc.setTenantId(TenantContext.getCurrentTenant());
        dbDoc = documentRepository.save(dbDoc);

        try (InputStream stream = file.getInputStream()) {
            String extractedText = tika.parseToString(stream);
            Document document = Document.from(extractedText);

            sendProgress(20, "Texte extrait, découpage en chunks...");

            DocumentSplitter splitter = DocumentSplitters.recursive(1000, 200);
            List<TextSegment> segments = splitter.split(document);

            int totalSegments = segments.size();
            for (int i = 0; i < totalSegments; i++) {
                TextSegment segment = segments.get(i);
                
                // Calculer l'embedding avec OpenAI
                Embedding embedding = embeddingModel.embed(segment).content();
                
                // Sauvegarder dans pgvector via JPA
                Chunk dbChunk = new Chunk();
                dbChunk.setDocument(dbDoc);
                dbChunk.setContent(segment.text());
                dbChunk.setChunkIndex(i);
                dbChunk.setEmbedding(embedding.vector());
                dbChunk.setTenantId(TenantContext.getCurrentTenant());
                chunkRepository.save(dbChunk);

                // Add metadata for LangChain4j filter
                segment.metadata().put("tenant_id", TenantContext.getCurrentTenant());
                segment.metadata().put("document_id", dbDoc.getId().toString());

                // Aussi ajouter dans le store LangChain4j pour faciliter la recherche
                embeddingStore.add(embedding, segment);

                // Envoyer la progression WebSocket
                int progress = 20 + (int) (((i + 1) / (float) totalSegments) * 80);
                sendProgress(progress, "Vectorisation en cours... (" + (i+1) + "/" + totalSegments + ")");
            }

            sendProgress(100, "Ingestion terminée avec succès.");
        } catch (Exception e) {
            sendProgress(0, "Erreur lors de l'ingestion : " + e.getMessage());
            throw e;
        }
    }

    private void sendProgress(int percentage, String message) {
        String jsonPayload = String.format("{\"percentage\": %d, \"message\": \"%s\"}", percentage, message);
        messagingTemplate.convertAndSend("/topic/document-progress", jsonPayload);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        jdbcTemplate.update("delete from langchain_chunks " +
                "where text in ( select content from chunks where document_id = ?)", documentId);
        jdbcTemplate.update("delete from chunks where document_id = ?", documentId);
        documentRepository.deleteById(documentId);
    }

    public List<ChunkResponse> searchSimilarChunks(String userQuery, int topK) {
        Embedding queryEmbedding = embeddingModel.embed(userQuery).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(0.4) // Lowered from 0.7 to 0.4 to prevent valid results from being filtered
                .filter(metadataKey("tenant_id").isEqualTo(TenantContext.getCurrentTenant()))
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

        return result.matches().stream()
                .map(match -> new ChunkResponse(match.embedded().text(), match.score()))
                .collect(Collectors.toList());
    }

    public Map<String, Object> chatWithDocuments(String userQuery, int topK, Long conversationId, String userId) {
        Conversation conversation;
        if(conversationId == null) {
            conversation = new Conversation();
            conversation.setUserId(userId);
            String title = userQuery.length() > 50 ? userQuery.substring(0, 47) + "..." : userQuery;
            conversation.setTitle(title);
            conversation = conversationRepository.save(conversation);
        }else {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("conversation not found"));
            if(!conversation.getUserId().equals(userId)) {
                throw new RuntimeException("Unauthorized to access this conversation");
            }
        }
        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversation(conversation);
        userMessage.setRole("USER");
        userMessage.setContent(userQuery);
        chatMessageRepository.save(userMessage);

        List<ChunkResponse> relevantChunks = searchSimilarChunks(userQuery, topK);
        String context = relevantChunks.stream().map(ChunkResponse::text).collect(Collectors.joining("\n\n"));

        StringBuilder history = new StringBuilder();
        if (conversation.getId() != null) {
            List<ChatMessage> previousMessages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            // Keep the last 6 messages to avoid context window explosion
            int startIndex = Math.max(0, previousMessages.size() - 6);
            for (int i = startIndex; i < previousMessages.size(); i++) {
                ChatMessage msg = previousMessages.get(i);
                if (msg.getId() != null && !msg.getId().equals(userMessage.getId())) {
                    history.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
                }
            }
        }

        String promptString = "You are a precise AI assistant. You must answer the user's question ONLY using the provided context. " +
                "If the context does not contain the answer, you must state that you don't have the information. Do not invent or guess.\n" +
                "IMPORTANT DISTINCTION: If the user asks about 'languages' or 'langues', explicitly differentiate between spoken/human languages (e.g., Arabic, French, English) and programming languages (e.g., Java, Python). Answer based precisely on what is asked.\n\n" +
                "Conversation History:\n{{history}}\n" +
                "Context:\n{{context}}\n\n" +
                "Question: {{question}}";

        PromptTemplate promptTemplate = PromptTemplate.from(promptString);

        Map<String, Object> variables = new HashMap<>();
        variables.put("history", history.toString().trim());
        variables.put("context", context);
        variables.put("question", userQuery);

        String promptText = promptTemplate.apply(variables).text();
        Long convId = conversation.getId();
        final Conversation finalConversation = conversation;
        final String currentTenant = TenantContext.getCurrentTenant();

        streamingChatLanguageModel.generate(promptText, new dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
            private StringBuilder fullResponseBuilder = new StringBuilder();

            @Override
            public void onNext(String token) {
                fullResponseBuilder.append(token);
                // Safe JSON escaping for the token
                String escapedToken = token.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
                messagingTemplate.convertAndSend("/topic/chat/" + convId, "{\"type\": \"token\", \"token\": \"" + escapedToken + "\"}");
            }

            @Override
            public void onComplete(dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response) {
                String aiResponse = fullResponseBuilder.toString();
                // Estimate tokens
                int estimatedTokens = aiResponse.length() / 4;

                // Save AI message first to get the ID
                ChatMessage aiMessage = new ChatMessage();
                aiMessage.setConversation(finalConversation);
                aiMessage.setRole("AI");
                aiMessage.setContent(aiResponse);
                aiMessage.setTokensUsed(estimatedTokens);
                aiMessage = chatMessageRepository.save(aiMessage);

                // Notify frontend that verification started
                try {
                    messagingTemplate.convertAndSend("/topic/chat/" + convId, "{\"type\": \"verifying\"}");
                } catch (Exception e) {}

                // Verify claims (Hallucination Guard) using pgvector
                HallucinationGuardService.GuardResult guardResult = guardService.verifyClaims(aiResponse, aiMessage.getId(), currentTenant);

                // Update AI message with results
                aiMessage.setConfidenceScore(guardResult.getConfidenceScore());
                aiMessage.setClaimAnalysis(guardResult.getClaimAnalysis());
                chatMessageRepository.save(aiMessage);

                // Publish Kafka event if hallucination detected (score < 50)
                if (guardResult.getConfidenceScore() < 50) {
                    try {
                        kafkaProducerService.sendHallucinationAlert(aiMessage.getId(), finalConversation.getUserId(), guardResult.getConfidenceScore());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String safeClaim = guardResult.getClaimAnalysis() != null ? mapper.writeValueAsString(guardResult.getClaimAnalysis()) : "null";
                    String payload = String.format("{\"type\": \"complete\", \"confidenceScore\": %s, \"claimAnalysis\": %s}", 
                            guardResult.getConfidenceScore(), safeClaim);
                    messagingTemplate.convertAndSend("/topic/chat/" + convId, payload);
                } catch(Exception e) {
                    messagingTemplate.convertAndSend("/topic/chat/" + convId, "{\"type\": \"complete\", \"confidenceScore\": null, \"claimAnalysis\": null}");
                }
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
                messagingTemplate.convertAndSend("/topic/chat/" + convId, "{\"type\": \"error\", \"message\": \"Stream error\"}");
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", convId);
        return result;
    }

    public DashboardStatsDTO getDashboardStats() {
        long totalDocs = documentRepository.count();
        long totalRequests = chatMessageRepository.countByRole("USER");
        long totalTokens = chatMessageRepository.sumAllTokensUsed();

        List<RequestHistoryItem> history = chatMessageRepository.countRequestsPerDay();
        List<TokenDistributionItem> distribution = List.of(new TokenDistributionItem("Llama 3",  totalTokens));
        List<com.intern.trustai.dto.ReliabilityStatItem> reliability = chatMessageRepository.averageScorePerDay();

        return new DashboardStatsDTO(totalDocs, totalRequests, totalTokens, history, distribution, reliability);
    }


    public byte[] generatePdfForConversation(Long conversationId, String userId) {
        Conversation conversation= conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("conversation not found"));
        if(!conversation.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to access this conversation");
        }
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("conversation: "+ conversation.getTitle(), titleFont);
            title.setSpacingAfter(20);
            document.add(title);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.GRAY);
            Font userFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, java.awt.Color.BLUE);
            Font aiFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new java.awt.Color(0, 153, 51));
            Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font redFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, java.awt.Color.RED);
            ObjectMapper mapper = new ObjectMapper();

            for (ChatMessage message : messages) {
                String roleStr = message.getRole().equals("USER") ? "User" : "TrustAI";
                Font roleFont = message.getRole().equals("USER") ? userFont : aiFont;

                String headerText = roleStr + " - " + message.getCreatedAt().format(formatter);
                if (message.getRole().equals("AI") && message.getConfidenceScore() != null) {
                    headerText += " (Confidence: " + message.getConfidenceScore() + "%)";
                }

                Paragraph header = new Paragraph(headerText, roleFont);
                header.setSpacingBefore(10);
                document.add(header);

                Paragraph content = new Paragraph(message.getContent(), contentFont);
                content.setSpacingAfter(5);
                document.add(content);

                // Add claim analysis if available
                if (message.getRole().equals("AI") && message.getClaimAnalysis() != null) {
                    try {
                        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(message.getClaimAnalysis());
                        com.fasterxml.jackson.databind.JsonNode claims = root.get("claims");
                        if (claims != null && claims.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode claim : claims) {
                                boolean isSupported = claim.has("isSupported") && claim.get("isSupported").asBoolean();
                                if (!isSupported) {
                                    String claimText = claim.has("text") ? claim.get("text").asText() : "Unknown claim";
                                    Paragraph redWarning = new Paragraph("⚠️ Unsupported/Hallucinated Claim: " + claimText, redFont);
                                    redWarning.setSpacingAfter(2);
                                    document.add(redWarning);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // Ignore parsing errors for PDF
                    }
                }
            }
            document.close();
            return baos.toByteArray();

        }catch(Exception e){
            throw new RuntimeException("Failed to generate PDF", e);

        }


    }
}