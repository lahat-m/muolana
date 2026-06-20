package com.lahat.muolana.shared.domain;

import java.util.UUID;

public record UserId(UUID id) {

    public static UserId of(UUID id) {
        return new UserId(id);
    }

    public static UserId of(String id) {
        return new UserId(UUID.fromString(id));
    }
}
