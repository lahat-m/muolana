package com.lahat.muolana.analytics.domain;

import java.time.Instant;
import java.util.UUID;

public record QueryLogVM(
        UUID id,
        UUID sessionId,
        UUID userId,
        String queryText,
        String outcome,
        String category,
        Long responseTimeMs,
        Instant createdAt
) {
}
