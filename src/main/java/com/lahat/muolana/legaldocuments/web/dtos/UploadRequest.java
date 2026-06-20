package com.lahat.muolana.legaldocuments.web.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UploadRequest(
        @NotBlank String title,
        @NotBlank String shortName,
        @NotNull String category,
        String versionLabel,
        String sourceUrl
) {
}

