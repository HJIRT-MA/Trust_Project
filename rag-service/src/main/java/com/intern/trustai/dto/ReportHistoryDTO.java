package com.intern.trustai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportHistoryDTO {
    private Long messageId;
    private String createdAt;
    private Integer confidenceScore;
    private String userEmail;
}
