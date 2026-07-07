package com.intern.trustai.repository;

import com.intern.trustai.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, Long> {
    List<Chunk> findByDocumentId(Long documentId);

  @Query(value = "SELECT content, 1 - (embedding <=> cast(:vector as vector)) as score " +
                   "FROM chunks ORDER BY embedding <=> cast(:vector as vector) LIMIT :topK", nativeQuery = true)
    List<Object[]> searchSimilarChunks(@Param("vector") String vector, @Param("topK") int topK);
}
