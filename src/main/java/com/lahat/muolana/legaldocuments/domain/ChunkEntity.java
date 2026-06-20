package com.lahat.muolana.legaldocuments.domain;

import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(schema = "legal_documents", name = "chunks")
class ChunkEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    protected ChunkEntity() {
    }

    ChunkEntity(UUID documentId, int chunkIndex, String content) {
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
    }

    UUID getId() {
        return id;
    }

    UUID getDocumentId() {
        return documentId;
    }

    int getChunkIndex() {
        return chunkIndex;
    }

    String getContent() {
        return content;
    }
}
