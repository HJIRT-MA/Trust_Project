package com.intern.trustai.service;

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
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public RagPipelineServiceImp(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore,
                                 DocumentRepository documentRepository, ChunkRepository chunkRepository,
                                 SimpMessagingTemplate messagingTemplate, JdbcTemplate jdbcTemplate,
                                 ChatLanguageModel chatLanguageModel, ConversationRepository conversationRepository,
                                 ChatMessageRepository chatMessageRepository) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.messagingTemplate = messagingTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.chatLanguageModel = chatLanguageModel;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
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

            DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
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
        String promptString = "You are a helpful and conversational AI assistant. " +
                "Please answer the user's question in a friendly and natural conversational way, using only the provided context. " +
                "If the answer is not contained in the context, politely let the user know that you don't have that information.\n\n" +
                "Context:\n{{context}}\n\n" +
                "Question: {{question}}";

        PromptTemplate promptTemplate = PromptTemplate.from(promptString);

        Map<String, Object> variables = new HashMap<>();
        variables.put("context", context);
        variables.put("question", userQuery);

        String aiResponse = chatLanguageModel.generate(promptTemplate.apply(variables).text());

        // Estimate tokens (roughly 1 token = 4 chars)
        int estimatedTokens = aiResponse.length() / 4;

        // Save AI message
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setConversation(conversation);
        aiMessage.setRole("AI");
        aiMessage.setContent(aiResponse);
        aiMessage.setTokensUsed(estimatedTokens);
        chatMessageRepository.save(aiMessage);

        Map<String, Object> result = new HashMap<>();
        result.put("response", aiResponse);
        result.put("conversationId", conversation.getId());
        return result;
    }

    public DashboardStatsDTO getDashboardStats() {
        long totalDocs = documentRepository.count();
        long totalRequests = chatMessageRepository.countByRole("USER");
        long totalTokens = chatMessageRepository.sumAllTokensUsed();

        List<RequestHistoryItem> history = chatMessageRepository.countRequestsPerDay();
        List<TokenDistributionItem> distribution = List.of(new TokenDistributionItem("Llama 3",  totalTokens));

        return new DashboardStatsDTO(totalDocs, totalRequests, totalTokens, history, distribution);
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

            for (ChatMessage message : messages) {
                String roleStr = message.getRole().equals("USER") ? "User" : "TrustAI";
                Font roleFont = message.getRole().equals(roleStr) ? userFont : aiFont;

                Paragraph header = new Paragraph(roleStr + " - " + message.getCreatedAt().format(formatter), roleFont);
                header.setSpacingBefore(10);
                document.add(header);

                Paragraph content = new Paragraph(message.getContent(), contentFont);
                content.setSpacingAfter(10);
                document.add(content);
            }
            document.close();
            return baos.toByteArray();

        }catch(Exception e){
            throw new RuntimeException("Failed to generate PDF", e);

        }


    }
}