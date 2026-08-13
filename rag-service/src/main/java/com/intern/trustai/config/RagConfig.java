package com.intern.trustai.config;


import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

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
        return OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text") // Or any other Ollama embedding model
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
                .table("langchain_chunks") // Use a separate table for LangChain4j to avoid JPA conflicts
                .dimension(768) // La taille du vecteur Ollama nomic-embed-text
                .build();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3:latest")
                .timeout(Duration.ofMinutes(5))
                .build();
   }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3:latest")
                .timeout(Duration.ofMinutes(5))
                .build();
    }
}
