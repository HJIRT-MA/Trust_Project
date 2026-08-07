package com.intern.trustai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalDocuments;
    private long totalRequests;
    private long totalTokens;
    private List<RequestHistoryItem> requestsHistory;
    private List<TokenDistributionItem> tokenDistribution;

}
