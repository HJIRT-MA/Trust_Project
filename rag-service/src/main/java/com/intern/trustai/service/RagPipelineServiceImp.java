package com.intern.trustai.service;

import com.intern.trustai.dto.ChunkResponse;
import com.intern.trustai.entity.Chunk;
import com.intern.trustai.repository.ChunkRepository;
import com.intern.trustai.repository.DocumentRepository;
import com.intern.trustai.repository.InteractionLogRepository;
import com.intern.trustai.entity.InteractionLog;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.tika.Tika;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.intern.trustai.security.TenantContext;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
public class RagPipelineServiceImp implements RagPipelineService {

    private final EmbeddingModel embeddingModel;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final InteractionLogRepository interactionLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Tika tika;

    public RagPipelineServiceImp(EmbeddingModel embeddingModel,
                                 DocumentRepository documentRepository, ChunkRepository chunkRepository,
                                 InteractionLogRepository interactionLogRepository,
                                 SimpMessagingTemplate messagingTemplate) {
        this.embeddingModel = embeddingModel;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.interactionLogRepository = interactionLogRepository;
        this.messagingTemplate = messagingTemplate;
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
                
                // Calculer l'embedding avec OpenAI ou Ollama
                dev.langchain4j.model.output.Response<Embedding> embeddingResponse = embeddingModel.embed(segment);
                Embedding embedding = embeddingResponse.content();

                // Log token usage
                if (embeddingResponse.tokenUsage() != null) {
                    InteractionLog log = new InteractionLog();
                    log.setTenantId(TenantContext.getCurrentTenant());
                    log.setQueryText("Ingestion: " + file.getOriginalFilename() + " (chunk " + i + ")");
                    log.setTokensUsed(embeddingResponse.tokenUsage().inputTokenCount());
                    log.setModelName("llama3.2:3b");
                    interactionLogRepository.save(log);
                }
                
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

    public List<ChunkResponse> searchSimilarChunks(String userQuery, int topK) {
        dev.langchain4j.model.output.Response<Embedding> embeddingResponse = embeddingModel.embed(userQuery);
        Embedding queryEmbedding = embeddingResponse.content();

        InteractionLog log = new InteractionLog();
        log.setTenantId(TenantContext.getCurrentTenant());
        log.setQueryText(userQuery);
        log.setTokensUsed(embeddingResponse.tokenUsage() != null ? embeddingResponse.tokenUsage().inputTokenCount() : 0);
        log.setModelName("llama3.2:3b");
        interactionLogRepository.save(log);

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(0.7)
                .filter(metadataKey("tenant_id").isEqualTo(TenantContext.getCurrentTenant()))
                .build();

        dev.langchain4j.store.embedding.EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

        return result.matches().stream()
                .map(match -> new ChunkResponse(match.embedded().text(), match.score()))
                .collect(Collectors.toList());


    }
}