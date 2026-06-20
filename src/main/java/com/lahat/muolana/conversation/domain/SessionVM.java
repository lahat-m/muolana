package com.lahat.muolana.conversation.domain;

import java.time.Instant;
import java.util.UUID;

public record SessionVM(UUID id, UUID userId, String title, SessionStatus status,
                        Instant lastActiveAt, Instant createdAt) {

    static SessionVM from(SessionEntity sessionEntity) {
        return new SessionVM(sessionEntity.getId(), sessionEntity.getUserId(), sessionEntity.getTitle(), sessionEntity.getStatus(),
                sessionEntity.getLastActiveAt(), sessionEntity.getCreatedAt());
    }
}
