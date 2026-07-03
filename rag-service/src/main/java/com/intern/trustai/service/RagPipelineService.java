package com.intern.trustai.service;

import com.intern.trustai.dto.ChunkResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface RagPipelineService {

    void ingestFile(MultipartFile file) throws Exception;
    List<ChunkResponse> searchSimilarChunks(String userQuery, int topK);
}
