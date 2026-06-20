package com.lahat.muolana.lawyers.web.dtos;

public record PatchLawyerRequest(
        String fullName,
        String lawFirmName,
        String locationCity,
        String feeType,
        String bio
) {
}

