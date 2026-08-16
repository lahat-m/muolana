package com.lahat.muolana.legaldocuments.domain;

import com.lahat.muolana.cloud.DocumentStorageService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
class DocumentSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentSeeder.class);

    private final LegalDocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final IngestionPipeline ingestionPipeline;

    DocumentSeeder(LegalDocumentRepository documentRepository,
                   DocumentStorageService storageService,
                   IngestionPipeline ingestionPipeline) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.ingestionPipeline = ingestionPipeline;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        for (SeedDoc seed : SeedDoc.ALL) {
            var existing = documentRepository.findByShortName(seed.shortName);
            if (existing.isPresent()) {
                DocumentStatus status = existing.get().getStatus();
                if (status == DocumentStatus.INGESTED || status == DocumentStatus.REJECTED) {
                    log.info("Already seeded ({}) — skipping: {}", status, seed.shortName);
                } else {
                    log.info("Re-queuing unfinished ingestion (status={}): {}", status, seed.shortName);
                    ingestionPipeline.ingest(existing.get().getId());
                }
                continue;
            }

            ClassPathResource resource = new ClassPathResource("documents/" + seed.filename);
            if (!resource.exists()) {
                log.warn("Classpath resource missing — documents/{}", seed.filename);
                continue;
            }

            try {
                byte[] bytes;
                try (var in = resource.getInputStream()) {
                    bytes = in.readAllBytes();
                }
                String objectKey = storageService.store(bytes, seed.filename);

                LegalDocumentEntity doc = new LegalDocumentEntity(
                        seed.title, seed.shortName, seed.category, seed.versionLabel, null, objectKey);
                doc.verify();
                LegalDocumentEntity saved = documentRepository.save(doc);
                ingestionPipeline.ingest(saved.getId());
                log.info("Uploaded to MinIO and queued ingestion: {}", seed.shortName);
            } catch (Exception e) {
                log.error("Failed to seed document '{}': {}", seed.shortName, e.getMessage(), e);
            }
        }
    }

    private record SeedDoc(
            String filename,
            String title,
            String shortName,
            DocumentCategory category,
            String versionLabel
    ) {
        static final List<SeedDoc> ALL = List.of(
                new SeedDoc(
                        "National-Security-Service-Act-2014.pdf",
                        "National Security Service Act 2014",
                        "NSS Act 2014",
                        DocumentCategory.ADMINISTRATIVE,
                        "2014")
        );
    }
}
