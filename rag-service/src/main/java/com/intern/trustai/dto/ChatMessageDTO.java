package com.intern.trustai.dto;

import com.intern.trustai.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageDTO(
        Long id,
        String role,
        String content,
        Integer tokensUsed,
        Integer confidenceScore,
        String claimAnalysis,
        LocalDateTime createdAt
) {
    public static ChatMessageDTO from(ChatMessage message) {
        return new ChatMessageDTO(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getTokensUsed(),
                message.getConfidenceScore(),
                message.getClaimAnalysis(),
                message.getCreatedAt()
        );
    }
}
