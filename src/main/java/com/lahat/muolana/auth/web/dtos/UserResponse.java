package com.lahat.muolana.auth.web.dtos;

import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName, String role, boolean isActive, String createdAt) {
}

