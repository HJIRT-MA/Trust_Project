package com.intern.trustai.dto;

import com.intern.trustai.entity.SmartContract;

import java.time.LocalDateTime;

public record SmartContractDTO(
        Long id,
        String name,
        String content,
        LocalDateTime createdAt,
        Integer globalRiskScore,
        String riskLevel,
        String auditor
) {
    public static SmartContractDTO from(SmartContract contract) {
        return new SmartContractDTO(
                contract.getId(),
                contract.getName(),
                contract.getContent(),
                contract.getCreatedAt(),
                contract.getGlobalRiskScore(),
                contract.getRiskLevel(),
                contract.getAuditor()
        );
    }
}
