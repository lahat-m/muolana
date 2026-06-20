package com.lahat.muolana.rag;

import java.util.List;

public record LegalAnswerResponse(
        String answer,
        List<CitationRef> citations,
        String category,
        String disclaimer) {
}
