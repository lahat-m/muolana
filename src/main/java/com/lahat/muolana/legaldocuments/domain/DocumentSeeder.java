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
            if (documentRepository.existsByShortName(seed.shortName)) {
                log.info("Already ingested — skipping: {}", seed.shortName);
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
                        "The-Transitional-Constitution-of-the-South-Sudan-2011.pdf",
                        "The Transitional Constitution of the Republic of South Sudan 2011",
                        "Constitution 2011",
                        DocumentCategory.CONSTITUTIONAL,
                        "2011"),
                new SeedDoc(
                        "Nationality-Act-2011.pdf",
                        "Nationality Act 2011",
                        "Nationality Act 2011",
                        DocumentCategory.CONSTITUTIONAL,
                        "2011"),
                new SeedDoc(
                        "National-Elections-Act-39-of-2012.pdf",
                        "National Elections Act No. 39 of 2012",
                        "National Elections Act 2012",
                        DocumentCategory.CONSTITUTIONAL,
                        "2012"),
                new SeedDoc(
                        "Land-Act-2009.pdf",
                        "Land Act of the Republic of South Sudan 2009",
                        "Land Act 2009",
                        DocumentCategory.LAND,
                        "2009"),
                new SeedDoc(
                        "Labour-Act-2017.pdf",
                        "Labour Act 2017",
                        "Labour Act 2017",
                        DocumentCategory.EMPLOYMENT,
                        "2017"),
                new SeedDoc(
                        "Advocates-Act-55-of-2013.pdf",
                        "Advocates Act No. 55 of 2013",
                        "Advocates Act 2013",
                        DocumentCategory.CIVIL,
                        "2013"),
                new SeedDoc(
                        "Local-Government-Act-2009.pdf",
                        "Local Government Act 2009",
                        "Local Government Act 2009",
                        DocumentCategory.ADMINISTRATIVE,
                        "2009"),
                new SeedDoc(
                        "National-Communication-Act-24-of-2012.pdf",
                        "National Communications Act No. 24 of 2012",
                        "National Communications Act 2012",
                        DocumentCategory.ADMINISTRATIVE,
                        "2012"),
                new SeedDoc(
                        "National-Security-Service-Act-2014.pdf",
                        "National Security Service Act 2014",
                        "NSS Act 2014",
                        DocumentCategory.ADMINISTRATIVE,
                        "2014")
        );
    }
}
