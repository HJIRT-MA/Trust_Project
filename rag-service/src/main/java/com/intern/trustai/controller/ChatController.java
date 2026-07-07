package com.intern.trustai.controller;

import com.intern.trustai.dto.ChatRequest;
import com.intern.trustai.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatController {

    private final ChatService chatService;
    private final com.intern.trustai.service.RagPipelineService ragPipelineService;

    public ChatController(ChatService chatService, com.intern.trustai.service.RagPipelineService ragPipelineService) {
        this.chatService = chatService;
        this.ragPipelineService = ragPipelineService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        return chatService.streamChat(request);
    }
}
