package com.lahat.muolana.legaldocuments.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
class VectorStoreWriter {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreWriter.class);
    private static final int BATCH_SIZE = 100;

    private final VectorStore vectorStore;

    VectorStoreWriter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Transactional
    public void add(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        int total = documents.size();
        log.info("Writing {} documents to vector store in batches of {}", total, BATCH_SIZE);

        for (int i = 0; i < total; i += BATCH_SIZE) {
            List<Document> batch = documents.subList(i, Math.min(i + BATCH_SIZE, total));
            log.debug("Adding batch {} to {}/{}", i, i + batch.size(), total);
            vectorStore.add(batch);
        }
    }
}
