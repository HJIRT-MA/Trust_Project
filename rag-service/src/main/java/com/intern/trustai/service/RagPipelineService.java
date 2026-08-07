package com.intern.trustai.service;

import com.intern.trustai.dto.ChunkResponse;
import org.springframework.web.multipart.MultipartFile;
import com.intern.trustai.dto.DashboardStatsDTO;
import java.io.IOException;
import java.util.List;

public interface RagPipelineService {

    void ingestFile(MultipartFile file) throws Exception;
    List<ChunkResponse> searchSimilarChunks(String userQuery, int topK);
    java.util.Map<String, Object> chatWithDocuments(String userQuery, int topK, Long conversationId, String userId);
    byte[] generatePdfForConversation(Long conversationId, String userId);
    DashboardStatsDTO getDashboardStats();
    void deleteDocument(Long documentId);
}
