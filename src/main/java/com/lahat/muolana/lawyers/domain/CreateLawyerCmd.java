package com.lahat.muolana.lawyers.domain;

public record CreateLawyerCmd(
        String fullName,
        String barNumber,
        String lawFirmName,
        String locationCity,
        FeeType feeType,
        String bio
) {
}
