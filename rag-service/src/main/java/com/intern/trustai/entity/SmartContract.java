package com.intern.trustai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "smart_contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmartContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "global_risk_score")
    private Integer globalRiskScore;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "auditor")
    private String auditor;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
