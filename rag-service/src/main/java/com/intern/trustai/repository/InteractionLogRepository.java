package com.intern.trustai.repository;

import com.intern.trustai.entity.InteractionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteractionLogRepository extends JpaRepository<InteractionLog, Integer> {

    @Query("SELECT COUNT(i) FROM InteractionLog i")
    long countTotalRequests();

    @Query("SELECT COALESCE(SUM(i.tokensUsed), 0) FROM InteractionLog i")
    long sumTotalTokens();

    @Query(value = "SELECT to_char(created_at, 'YYYY-MM-DD') as date, COUNT(*) as count " +
            "FROM interaction_log " +
            "WHERE tenant_id = current_setting('app.current_tenant', true) " +
            "GROUP BY to_char(created_at, 'YYYY-MM-DD') " +
            "ORDER BY date ASC " +
            "LIMIT 30", nativeQuery = true)
    List<Object[]> getRequestsHistoryNative();

    // Workaround for tenant filtering with native queries, or just use JPQL which handles the filter automatically:
    @Query("SELECT FUNCTION('TO_CHAR', i.createdAt, 'YYYY-MM-DD') as date, COUNT(i) as count " +
            "FROM InteractionLog i " +
            "GROUP BY FUNCTION('TO_CHAR', i.createdAt, 'YYYY-MM-DD') " +
            "ORDER BY date ASC")
    List<Object[]> getRequestsHistory();

    @Query("SELECT i.modelName as model, COALESCE(SUM(i.tokensUsed), 0) as tokens " +
            "FROM InteractionLog i " +
            "GROUP BY i.modelName")
    List<Object[]> getTokenDistribution();
}
