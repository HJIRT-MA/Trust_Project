package com.intern.trustai.dto;

import com.intern.trustai.entity.Document;

import java.time.LocalDateTime;

public record DocumentDTO(
        Long id,
        String filename,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt
) {
    public static DocumentDTO from(Document document) {
        return new DocumentDTO(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getUploadedAt()
        );
    }
}
