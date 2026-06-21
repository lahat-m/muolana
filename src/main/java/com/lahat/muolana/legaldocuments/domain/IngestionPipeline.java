package com.lahat.muolana.legaldocuments.domain;

import com.lahat.muolana.cloud.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
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
    private final TokenTextSplitter splitter;

    IngestionPipeline(LegalDocumentRepository documentRepository,
                      IngestionStatusUpdater statusUpdater,
                      VectorStoreWriter vectorStoreWriter,
                      DocumentStorageService storageService) {
        this.documentRepository = documentRepository;
        this.statusUpdater = statusUpdater;
        this.vectorStoreWriter = vectorStoreWriter;
        this.storageService = storageService;
        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(512)
                .withMinChunkSizeChars(64)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
    }

    @Async
    public void ingest(UUID docId) {
        LegalDocumentEntity doc = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + docId));

        String objectKey = doc.getFilePath();
        if (objectKey == null || objectKey.isBlank()) {
            log.warn("No file stored for document {}", docId);
            return;
        }

        try {
            statusUpdater.markChunking(docId);

            byte[] bytes;
            try (var stream = storageService.download(objectKey)) {
                bytes = stream.readAllBytes();
            }
            List<Document> pages = new TikaDocumentReader(new ByteArrayResource(bytes)).get();
            String fullText = pages.stream()
                    .map(Document::getText)
                    .reduce("", (a, b) -> a + "\n" + b)
                    .trim();

            Document source = Document.builder()
                    .text(fullText)
                    .metadata(Map.of(
                            "document_id", doc.getId().toString(),
                            "title", doc.getTitle(),
                            "short_name", doc.getShortName(),
                            "category", doc.getCategory().name(),
                            "source_url", doc.getSourceUrl() != null ? doc.getSourceUrl() : ""))
                    .build();

            List<Document> chunks = splitter.apply(List.of(source));
            List<Document> toStore = statusUpdater.saveChunks(docId, chunks);

            statusUpdater.markEmbedding(docId);

            vectorStoreWriter.add(toStore);

            statusUpdater.markIngested(docId);

            log.info("Ingested document {} — {} chunks stored", docId, toStore.size());

        } catch (Exception exception) {
            log.error("Ingestion failed for document {}", docId, exception);
            statusUpdater.markFailed(docId);
        }
    }
}
