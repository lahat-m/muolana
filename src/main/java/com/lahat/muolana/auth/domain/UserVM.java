package com.lahat.muolana.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record UserVM(UUID id, String email, String fullName, String role, boolean isActive, Instant createdAt) {
}
