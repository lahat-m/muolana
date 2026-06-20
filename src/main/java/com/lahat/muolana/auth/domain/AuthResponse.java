package com.lahat.muolana.auth.domain;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {
}
