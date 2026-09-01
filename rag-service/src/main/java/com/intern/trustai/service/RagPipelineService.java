package com.intern.trustai.service;

import com.intern.trustai.dto.ChunkResponse;
import org.springframework.web.multipart.MultipartFile;
import com.intern.trustai.dto.DashboardStatsDTO;
import com.intern.trustai.entity.ChatMessage;
import com.intern.trustai.entity.Conversation;
import com.intern.trustai.entity.Document;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface RagPipelineService {

    void ingestFile(MultipartFile file) throws Exception;
    List<ChunkResponse> searchSimilarChunks(String userQuery, int topK);
    Map<String, Object> chatWithDocuments(String userQuery, int topK, Long conversationId, String userId);
    byte[] generatePdfForConversation(Long conversationId, String userId);
    DashboardStatsDTO getDashboardStats();
    void deleteDocument(Long documentId);
    
    List<Document> getAllDocuments();
    List<Conversation> getUserConversations(String userId);
    List<ChatMessage> getConversationMessages(Long conversationId, String userId);
    void deleteConversation(Long conversationId, String userId);
}
