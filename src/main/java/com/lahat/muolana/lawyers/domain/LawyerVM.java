package com.lahat.muolana.lawyers.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LawyerVM(
        UUID id,
        String fullName,
        String barNumber,
        String lawFirmName,
        String locationCity,
        String feeType,
        String bio,
        boolean isVerified,
        String status,
        BigDecimal averageRating,
        int reviewCount,
        Instant createdAt,
        List<String> specialisations,
        List<ContactMethodVM> contactMethods
) {
}
