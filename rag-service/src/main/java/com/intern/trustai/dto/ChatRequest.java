package com.intern.trustai.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private String query;
    private List<ChatMessage> history;

    @Data
    public static class ChatMessage {
        private String role; // "user" or "assistant"
        private String content;
    }
}
