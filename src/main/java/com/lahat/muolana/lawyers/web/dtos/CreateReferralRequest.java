package com.lahat.muolana.lawyers.web.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateReferralRequest(
        @NotNull UUID sessionId,
        @NotNull String channel,
        @AssertTrue(message = "consentGiven must be true") boolean consentGiven
) {
}

