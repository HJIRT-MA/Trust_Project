package com.intern.trustai.dto;

import com.intern.trustai.entity.Conversation;

import java.time.LocalDateTime;

public record ConversationDTO(
        Long id,
        String title,
        LocalDateTime createdAt
) {
    public static ConversationDTO from(Conversation conversation) {
        return new ConversationDTO(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt()
        );
    }
}
