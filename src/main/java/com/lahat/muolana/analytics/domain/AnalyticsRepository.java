package com.lahat.muolana.analytics.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface AnalyticsRepository extends JpaRepository<QueryLogEntity, UUID> {

    Page<QueryLogEntity> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);

    Page<QueryLogEntity> findByCreatedAtBetweenAndOutcome(Instant from, Instant to, QueryOutcome outcome, Pageable pageable);

    @Query("SELECT COUNT(q) FROM QueryLogEntity q WHERE q.createdAt BETWEEN :from AND :to")
    long countByPeriod(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(q) FROM QueryLogEntity q WHERE q.createdAt BETWEEN :from AND :to AND q.outcome = :outcome")
    long countByPeriodAndOutcome(@Param("from") Instant from, @Param("to") Instant to, @Param("outcome") QueryOutcome outcome);

    @Query("""
            SELECT q.category, COUNT(q) as cnt
            FROM QueryLogEntity q
            WHERE q.createdAt BETWEEN :from AND :to AND q.category IS NOT NULL
            GROUP BY q.category
            ORDER BY cnt DESC
            """)
    List<Object[]> findTopCategories(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("""
            SELECT q.queryText, COUNT(q) as cnt
            FROM QueryLogEntity q
            WHERE q.createdAt BETWEEN :from AND :to AND q.outcome = 'NOT_FOUND'
            GROUP BY q.queryText
            ORDER BY cnt DESC
            """)
    List<Object[]> findKnowledgeGaps(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT q.sessionId) FROM QueryLogEntity q WHERE q.createdAt BETWEEN :from AND :to")
    long countDistinctSessions(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "SELECT AVG(cnt) FROM (SELECT COUNT(*) as cnt FROM analytics.query_logs WHERE created_at BETWEEN :from AND :to GROUP BY session_id) sub",
            nativeQuery = true)
    Double avgMessagesPerSession(@Param("from") Instant from, @Param("to") Instant to);
}
