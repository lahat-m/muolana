package com.lahat.muolana.lawyers.web.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLawyerRequest(
        @NotBlank String fullName,
        @NotBlank String barNumber,
        String lawFirmName,
        String locationCity,
        @NotNull String feeType,
        String bio
) {
}

