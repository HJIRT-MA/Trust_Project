package com.intern.trustai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_findings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "smart_contract_id", nullable = false)
    private SmartContract smartContract;

    @Column(nullable = false)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", name = "code_snippet")
    private String codeSnippet;

    @Column(name = "validated_by_rules")
    private boolean validatedByRules;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "swc_id")
    private String swcId;

    @Column(name = "swc_title")
    private String swcTitle;

    @Column(columnDefinition = "TEXT", name = "enriched_explanation")
    private String enrichedExplanation;

    @Column(columnDefinition = "TEXT", name = "vulnerable_example")
    private String vulnerableExample;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
