package com.lahat.muolana.legaldocuments.domain;

import com.lahat.muolana.shared.AssertUtil;
import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(schema = "legal_documents", name = "documents")
class LegalDocumentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "short_name", nullable = false, length = 100)
    private String shortName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private DocumentCategory category;

    @Column(name = "version_label", length = 50)
    private String versionLabel;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    protected LegalDocumentEntity() {
    }

    LegalDocumentEntity(String title, String shortName, DocumentCategory category,
                        String versionLabel, String sourceUrl, String filePath) {
        this.title = AssertUtil.requireNotBlank(title, "title required");
        this.shortName = AssertUtil.requireNotBlank(shortName, "shortName required");
        this.category = AssertUtil.requireNotNull(category, "category required");
        this.versionLabel = versionLabel;
        this.sourceUrl = sourceUrl;
        this.filePath = filePath;
    }

    void verify() {
        this.status = DocumentStatus.VERIFIED;
        this.rejectionReason = null;
    }

    void reject(String reason) {
        this.status = DocumentStatus.REJECTED;
        this.rejectionReason = reason;
    }

    void markChunking() {
        this.status = DocumentStatus.CHUNKING;
    }

    void markEmbedding() {
        this.status = DocumentStatus.EMBEDDING;
    }

    void markIngested() {
        this.status = DocumentStatus.INGESTED;
    }

    UUID getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    String getShortName() {
        return shortName;
    }

    DocumentCategory getCategory() {
        return category;
    }

    String getVersionLabel() {
        return versionLabel;
    }

    String getSourceUrl() {
        return sourceUrl;
    }

    String getFilePath() {
        return filePath;
    }

    DocumentStatus getStatus() {
        return status;
    }

    String getRejectionReason() {
        return rejectionReason;
    }
}
