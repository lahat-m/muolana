package com.lahat.muolana.rag;

import java.util.UUID;

public record ChunkMatch(UUID chunkId, String content, String sourceRef, String category, double score, String sourceUrl) {
}
