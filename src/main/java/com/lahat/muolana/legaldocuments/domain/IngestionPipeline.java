package com.lahat.muolana.legaldocuments.domain;

import com.lahat.muolana.cloud.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    private final LegalDocumentRepository documentRepository;
    private final IngestionStatusUpdater statusUpdater;
    private final VectorStoreWriter vectorStoreWriter;
    private final DocumentStorageService storageService;
    private final DoclingGateway doclingGateway;

    IngestionPipeline(LegalDocumentRepository documentRepository,
                      IngestionStatusUpdater statusUpdater,
                      VectorStoreWriter vectorStoreWriter,
                      DocumentStorageService storageService,
                      DoclingGateway doclingGateway) {
        this.documentRepository = documentRepository;
        this.statusUpdater = statusUpdater;
        this.vectorStoreWriter = vectorStoreWriter;
        this.storageService = storageService;
        this.doclingGateway = doclingGateway;
    }

    @Async("ingestionExecutor")
    public void ingest(UUID docId) {
        LegalDocumentEntity doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + docId));

        String objectKey = doc.getFilePath();
        if (objectKey == null || objectKey.isBlank()) {
            log.warn("No file stored for document {}", docId);
            return;
        }

        try {
            log.info("[INGESTION_START] Document: {} (ID: {})", doc.getShortName(), docId);

            log.info("[PARSING] Downloading and parsing document: {}", doc.getShortName());
            statusUpdater.markChunking(docId);

            byte[] bytes;
            try (var stream = storageService.download(objectKey)) {
                bytes = stream.readAllBytes();
            }

            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return doc.getShortName() + ".pdf";
                }
            };

            Map<String, Object> appMeta = Map.of(
                    "document_id", doc.getId().toString(),
                    "title", doc.getTitle(),
                    "short_name", doc.getShortName(),
                    "category", doc.getCategory().name(),
                    "source_url", doc.getSourceUrl() != null ? doc.getSourceUrl() : "");

            List<Document> chunks = doclingGateway.chunk(resource, appMeta);

            if (chunks.isEmpty()) {
                log.warn("Docling returned no chunks for document {} — file may be empty or unreadable", docId);
                statusUpdater.markFailed(docId);
                return;
            }

            log.info("[CHUNKING] Saving parsed chunks for document: {}", doc.getShortName());
            List<Document> toStore = statusUpdater.saveChunks(docId, chunks);

            log.info("[EMBEDDING] Generating embeddings and storing in vector store for document: {}", doc.getShortName());
            statusUpdater.markEmbedding(docId);
            vectorStoreWriter.add(toStore);

            statusUpdater.markIngested(docId);

            log.info("[DONE] Ingestion completed successfully for document: {} ({} chunks stored)", doc.getShortName(), toStore.size());

        } catch (Exception exception) {
            log.error("[FAILED] Ingestion failed for document: {} (ID: {})", doc.getShortName(), docId, exception);
            statusUpdater.markFailed(docId);
        }
    }
}
