package com.intern.trustai.config;


import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RagConfig {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${spring.datasource.url}")
    private String dbUrl;
    @Value("${spring.datasource.username}")
    private String dbUsername;
    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(openAiApiKey)
                .modelName("text-embedding-3-small")
                .timeout(Duration.ofSeconds(15))
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // Le PgVectorEmbeddingStore gère automatiquement les requêtes cosinus
        return PgVectorEmbeddingStore.builder()
                .host("localhost") // Ou extraire du dbUrl
                .port(5432)
                .database("trustaidb")
                .user(dbUsername)
                .password(dbPassword)
                .table("chunks") // Le nom de la table créée à la Semaine 3
                .dimension(1536) // La taille du vecteur OpenAI
                .build();
    }

}
