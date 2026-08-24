package com.intern.trustai.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_report_signatures")
@Data
@NoArgsConstructor
public class AuditReportSignature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "smart_contract_id", nullable = false)
    private SmartContract smartContract;

    @Column(name = "pdf_hash", nullable = false)
    private String pdfHash;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String signature;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
