package com.intern.trustai.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table (name = "chunks")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Chunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    // Mapping pgvector : On utilise float[] avec une définition de colonne native
    @Column(columnDefinition = "vector(384)")
    private float[] embedding;

    // Getters, Setters, et Constructeurs
}
