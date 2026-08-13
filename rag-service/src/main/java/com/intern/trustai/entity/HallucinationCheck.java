package com.intern.trustai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hallucination_checks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallucinationCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "claim_text", columnDefinition = "TEXT", nullable = false)
    private String claimText;

    @Column(nullable = false)
    private String status; // VERIFIÉ, INCERTAIN, NON VERIFIÉ

    @Column(name = "similarity_score")
    private Double similarityScore;

    @Column(name = "source_chunks", columnDefinition = "TEXT")
    private String sourceChunks; // JSON representing top chunks
}
