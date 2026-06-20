package com.lahat.muolana.auth.web.dtos;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {
}

