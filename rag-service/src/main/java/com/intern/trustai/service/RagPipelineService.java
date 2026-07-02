package com.intern.trustai.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface RagPipelineService {

    void ingestFile(MultipartFile file) throws IOException;
    List<String> searchSimilarChunks(String userQuery,int topK);
}
