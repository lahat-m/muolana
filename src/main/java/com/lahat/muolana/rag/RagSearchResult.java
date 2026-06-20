package com.lahat.muolana.rag;

import java.util.List;

public record RagSearchResult(List<ChunkMatch> chunks, boolean guardTriggered) {
}
