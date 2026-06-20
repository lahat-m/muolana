package com.lahat.muolana.analytics.domain;

import java.util.UUID;

public record RecordQueryCmd(UUID sessionId, UUID userId, String queryText,
                             QueryOutcome outcome, String category, long responseTimeMs) {
}
