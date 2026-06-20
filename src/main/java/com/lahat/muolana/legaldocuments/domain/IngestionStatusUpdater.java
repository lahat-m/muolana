package com.lahat.muolana.legaldocuments.domain;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
class IngestionStatusUpdater {

    private final LegalDocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;

    IngestionStatusUpdater(LegalDocumentRepository documentRepository,
                           ChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    void markChunking(UUID docId) {
        chunkRepository.deleteByDocumentId(docId);
        LegalDocumentEntity doc = documentRepository.getById(docId);
        doc.markChunking();
        documentRepository.save(doc);
    }

    List<Document> saveChunks(UUID docId, List<Document> chunks) {
        List<Document> toStore = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            ChunkEntity entity = chunkRepository.save(new ChunkEntity(docId, i, chunk.getText()));
            Map<String, Object> meta = new HashMap<>(chunk.getMetadata());
            meta.put("chunk_index", i);
            toStore.add(Document.builder()
                    .id(entity.getId().toString())
                    .text(chunk.getText())
                    .metadata(meta)
                    .build());
        }
        return toStore;
    }

    void markEmbedding(UUID docId) {
        LegalDocumentEntity doc = documentRepository.getById(docId);
        doc.markEmbedding();
        documentRepository.save(doc);
    }

    void markIngested(UUID docId) {
        LegalDocumentEntity doc = documentRepository.getById(docId);
        doc.markIngested();
        documentRepository.save(doc);
    }

    void markFailed(UUID docId) {
        documentRepository.findById(docId).ifPresent(doc -> {
            doc.verify();
            documentRepository.save(doc);
        });
    }
}
