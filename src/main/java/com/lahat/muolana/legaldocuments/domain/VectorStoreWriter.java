package com.lahat.muolana.legaldocuments.domain;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
class VectorStoreWriter {

    private final VectorStore vectorStore;

    VectorStoreWriter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Transactional
    public void add(List<Document> documents) {
        vectorStore.add(documents);
    }
}
