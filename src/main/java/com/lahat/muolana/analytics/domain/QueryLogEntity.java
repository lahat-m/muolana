package com.lahat.muolana.analytics.domain;

import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(schema = "analytics", name = "query_logs")
public class QueryLogEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private QueryOutcome outcome;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    protected QueryLogEntity() {
    }

    public QueryLogEntity(UUID sessionId, UUID userId, String queryText, QueryOutcome outcome, String category, Long responseTimeMs) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.queryText = queryText;
        this.outcome = outcome;
        this.category = category;
        this.responseTimeMs = responseTimeMs;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getQueryText() {
        return queryText;
    }

    public QueryOutcome getOutcome() {
        return outcome;
    }

    public String getCategory() {
        return category;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
}
