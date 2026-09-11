package com.intern.trustai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The LangChain4j PgVectorEmbeddingStore manages its own `langchain_chunks` table
 * outside of JPA (see RagConfig#embeddingStore) to avoid clashing with the `chunks`
 * entity, so there is no Spring Data repository for it. This class is the single,
 * named place for the raw SQL needed to keep it in sync with `chunks`, instead of
 * that SQL living inline in a business service next to unrelated JPA calls.
 */
@Repository
public class LangchainEmbeddingStoreRepository {

    private final JdbcTemplate jdbcTemplate;

    public LangchainEmbeddingStoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteEmbeddingsForDocument(Long documentId) {
        jdbcTemplate.update("delete from langchain_chunks " +
                "where text in (select content from chunks where document_id = ?)", documentId);
    }
}
