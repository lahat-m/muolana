package com.lahat.muolana.lawyers.domain;

public record UpdateLawyerCmd(
        String fullName,
        String lawFirmName,
        String locationCity,
        FeeType feeType,
        String bio
) {
}
