package com.lahat.muolana.legaldocuments.domain;

import java.time.Instant;
import java.util.UUID;

public record LegalDocumentVM(
        UUID id,
        String title,
        String shortName,
        String category,
        String versionLabel,
        String sourceUrl,
        String filePath,
        String status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
}
