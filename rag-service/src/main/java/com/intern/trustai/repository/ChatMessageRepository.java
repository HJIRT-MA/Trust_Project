package com.intern.trustai.repository;

import com.intern.trustai.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    void deleteByConversationId(Long conversationId);

    long countByRole(String role);

    @Query("SELECT COALESCE(SUM(c.tokensUsed), 0) FROM ChatMessage c")
    long sumAllTokensUsed();

    @Query("SELECT new com.intern.trustai.dto.RequestHistoryItem(TO_CHAR(c.createdAt, 'YYYY-MM-DD'), COUNT(c)) " +
            "FROM ChatMessage c WHERE c.role = 'USER' " +
            "GROUP BY TO_CHAR(c.createdAt, 'YYYY-MM-DD') " +
            "ORDER BY TO_CHAR(c.createdAt, 'YYYY-MM-DD') ASC")
    List<com.intern.trustai.dto.RequestHistoryItem> countRequestsPerDay();

    @Query("SELECT new com.intern.trustai.dto.ReliabilityStatItem(TO_CHAR(c.createdAt, 'YYYY-MM-DD'), AVG(c.confidenceScore)) " +
            "FROM ChatMessage c WHERE c.role = 'AI' AND c.confidenceScore IS NOT NULL " +
            "GROUP BY TO_CHAR(c.createdAt, 'YYYY-MM-DD') " +
            "ORDER BY TO_CHAR(c.createdAt, 'YYYY-MM-DD') ASC")
    List<com.intern.trustai.dto.ReliabilityStatItem> averageScorePerDay();
}
