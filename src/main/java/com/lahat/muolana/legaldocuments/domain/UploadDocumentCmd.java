package com.lahat.muolana.legaldocuments.domain;

public record UploadDocumentCmd(
        String title,
        String shortName,
        DocumentCategory category,
        String versionLabel,
        String sourceUrl,
        String filePath
) {
}
