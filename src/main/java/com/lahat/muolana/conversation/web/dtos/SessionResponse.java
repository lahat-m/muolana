package com.lahat.muolana.conversation.web.dtos;

import com.lahat.muolana.conversation.domain.SessionVM;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID id, UUID userId, String title, String status,
                              Instant lastActiveAt, Instant createdAt) {

    public static SessionResponse from(SessionVM vm) {
        return new SessionResponse(vm.id(), vm.userId(), vm.title(), vm.status().name(),
                vm.lastActiveAt(), vm.createdAt());
    }
}
