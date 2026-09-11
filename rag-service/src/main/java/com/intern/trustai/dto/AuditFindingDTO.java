package com.intern.trustai.dto;

import com.intern.trustai.entity.AuditFinding;

import java.time.LocalDateTime;

public record AuditFindingDTO(
        Long id,
        String severity,
        String title,
        String description,
        String codeSnippet,
        boolean validatedByRules,
        LocalDateTime createdAt,
        String swcId,
        String swcTitle,
        String enrichedExplanation,
        String vulnerableExample
) {
    public static AuditFindingDTO from(AuditFinding finding) {
        return new AuditFindingDTO(
                finding.getId(),
                finding.getSeverity(),
                finding.getTitle(),
                finding.getDescription(),
                finding.getCodeSnippet(),
                finding.isValidatedByRules(),
                finding.getCreatedAt(),
                finding.getSwcId(),
                finding.getSwcTitle(),
                finding.getEnrichedExplanation(),
                finding.getVulnerableExample()
        );
    }
}
