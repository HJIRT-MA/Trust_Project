package com.intern.trustai.service;

import com.intern.trustai.dto.ChatRequest;
import com.intern.trustai.dto.ChunkResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final StreamingChatLanguageModel chatLanguageModel;
    private final RagPipelineService ragPipelineService;

    public ChatService(StreamingChatLanguageModel chatLanguageModel, RagPipelineService ragPipelineService) {
        this.chatLanguageModel = chatLanguageModel;
        this.ragPipelineService = ragPipelineService;
    }

    public SseEmitter streamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        // 1. Retrieve similar chunks
        List<ChunkResponse> relevantChunks = ragPipelineService.searchSimilarChunks(request.getQuery(), 3);
        String contextText = relevantChunks.stream()
                .map(ChunkResponse::text)
                .collect(Collectors.joining("\n\n---\n\n"));

        // Build sources JSON to send as the first SSE event so frontend can display them
        sendSourcesEvent(emitter, relevantChunks);

        // 2. Build ChatMessage history
        List<ChatMessage> messages = new ArrayList<>();
        
        // System Prompt
        String systemPrompt = "Tu es un assistant IA expert en analyse documentaire. " +
                "Réponds à la question de l'utilisateur EN TE BASANT UNIQUEMENT sur le contexte fourni ci-dessous. " +
                "Si la réponse ne se trouve pas dans le contexte, dis simplement que tu ne sais pas.\n\n" +
                "CONTEXTE:\n" + contextText;
        messages.add(new SystemMessage(systemPrompt));

        // Append previous history
        if (request.getHistory() != null) {
            for (ChatRequest.ChatMessage hm : request.getHistory()) {
                if ("user".equals(hm.getRole())) {
                    messages.add(new UserMessage(hm.getContent()));
                } else if ("assistant".equals(hm.getRole())) {
                    messages.add(new AiMessage(hm.getContent()));
                }
            }
        }

        // Add current query
        messages.add(new UserMessage(request.getQuery()));

        // 3. Call LLM Streaming
        chatLanguageModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                try {
                    // Send each token to the client
                    emitter.send(SseEmitter.event().name("message").data(token.replace("\n", "\\n")));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
                try {
                    emitter.send(SseEmitter.event().name("error").data("Error generating response: " + error.getMessage()));
                    emitter.completeWithError(error);
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }
        });

        return emitter;
    }

    private void sendSourcesEvent(SseEmitter emitter, List<ChunkResponse> chunks) {
        try {
            // We serialize sources manually for simplicity, or use Jackson
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < chunks.size(); i++) {
                ChunkResponse c = chunks.get(i);
                // Escape quotes and newlines
                String text = c.text().replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
                sb.append("{\"text\":\"").append(text).append("\",\"score\":").append(c.score()).append("}");
                if (i < chunks.size() - 1) sb.append(",");
            }
            sb.append("]");
            
            emitter.send(SseEmitter.event().name("sources").data(sb.toString()));
        } catch (IOException e) {
            // Ignore
        }
    }
}
