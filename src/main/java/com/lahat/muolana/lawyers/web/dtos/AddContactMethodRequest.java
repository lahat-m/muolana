package com.lahat.muolana.lawyers.web.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddContactMethodRequest(
        @NotNull String channel,
        @NotBlank String value,
        boolean isPrimary
) {
}

