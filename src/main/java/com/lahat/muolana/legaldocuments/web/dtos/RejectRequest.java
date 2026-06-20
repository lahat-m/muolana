package com.lahat.muolana.legaldocuments.web.dtos;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(@NotBlank String rejectionReason) {
}

