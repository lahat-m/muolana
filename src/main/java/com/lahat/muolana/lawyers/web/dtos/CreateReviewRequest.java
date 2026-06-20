package com.lahat.muolana.lawyers.web.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record CreateReviewRequest(
        UUID sessionId,
        @Min(1) @Max(5) int rating,
        String reviewText,
        boolean isAnonymous
) {
}

