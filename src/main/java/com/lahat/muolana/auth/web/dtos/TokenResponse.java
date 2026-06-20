package com.lahat.muolana.auth.web.dtos;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}

