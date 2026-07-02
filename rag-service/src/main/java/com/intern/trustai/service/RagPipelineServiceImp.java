package com.intern.trustai.service;


import com.intern.trustai.entity.Document;
import com.intern.trustai.repository.DocumentRepository;
import dev.ai4j.openai4j.embedding.Embedding;
import dev.ai4j.openai4j.embedding.EmbeddingModel;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagPipelineServiceImp implements RagPipelineService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore embeddingStore;
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
        // 1. Extraire le texte avec Tika (Gère les PDF, Word, etc.)
        try (InputStream stream = file.getInputStream()) {
            String extractedText = tika.parseToString(stream);
            Document document = Document.from(extractedText);

            // 2. Découpage intelligent (Chunks sémantiques)
            // Utilise un tokenizer qui respecte les phrases et les paragraphes,
            // avec un overlap (chevauchement) pour ne pas perdre le contexte entre deux chunks.
            DocumentSplitter splitter = DocumentSplitters.recursive(
                    500, // Taille max du chunk (en tokens)
                    50   // Overlap (chevauchement)
            );
            List<TextSegment> segments = splitter.split(document);

            // 3. Calculer les embeddings et sauvegarder dans pgvector
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);
        }
    }

    public List<String> searchSimilarChunks(String userQuery, int topK) {
        // 1. Transformer la question de l'utilisateur en vecteur
        Embedding queryEmbedding = embeddingModel.embed(userQuery).content();

        // 2. Préparer la requête de recherche sémantique
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .minScore(0.7) // Seuil de similarité cosinus minimum (0 = rien en commun, 1 = copie exacte)
                .build();

        // 3. Exécuter la recherche dans pgvector
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

        // 4. Extraire uniquement le texte des segments trouvés pour le renvoyer à l'API
        return result.matches().stream()
                .map(EmbeddingMatch::embedded)
                .map(TextSegment::text)
                .collect(Collectors.toList());
    }

}
