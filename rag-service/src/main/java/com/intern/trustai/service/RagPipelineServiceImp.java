package com.intern.trustai.service;

import com.intern.trustai.dto.ChunkResponse;

// 1. Corrected LangChain4j Document Import
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagPipelineServiceImp implements RagPipelineService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore; // Added generic here
    private final Tika tika;

    public RagPipelineServiceImp(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.tika = new Tika();
    }

    /**
     * INGESTION : Upload -> Tika -> Splitter -> Embeddings -> pgvector
     */
    public void ingestFile(MultipartFile file) throws Exception {
        try (InputStream stream = file.getInputStream()) {
            String extractedText = tika.parseToString(stream);
            Document document = Document.from(extractedText);

            DocumentSplitter splitter = DocumentSplitters.recursive(
                    500, // Taille max du chunk (en tokens)
                    50   // Overlap (chevauchement)
            );
            List<TextSegment> segments = splitter.split(document);

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
        }
    }

    /**
     * RETRIEVAL : Query -> Vector -> Recherche Cosinus -> Retourne Text + Score
     */
    // 2. Updated method signature to return List<ChunkResponse>
    public List<ChunkResponse> searchSimilarChunks(String userQuery, int topK) {

        Embedding queryEmbedding = embeddingModel.embed(userQuery).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(0.7)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

        // 3. Removed the double-return. This is the only return needed now.
        return result.matches().stream()
                .map(match -> new ChunkResponse(match.embedded().text(), match.score()))
                .collect(Collectors.toList());
    }
}