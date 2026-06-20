package com.lahat.muolana.legaldocuments.domain;

import java.util.UUID;

public record ChunkVM(UUID id, UUID documentId, int chunkIndex, String content) {
}
