package com.intern.trustai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalDocuments;
    private long totalRequests;
    private long totalTokens;
    private List<HistoryEntry> requestsHistory;
    private List<TokenDistributionEntry> tokenDistribution;

    @Data
    @Builder
    public static class HistoryEntry {
        private String date;
        private long count;
    }

    @Data
    @Builder
    public static class TokenDistributionEntry {
        private String model;
        private long tokens;
    }
}
