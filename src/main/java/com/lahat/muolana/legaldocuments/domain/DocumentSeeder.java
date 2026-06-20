package com.lahat.muolana.legaldocuments.domain;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Component
@Order(2)
class DocumentSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentSeeder.class);

    private final LegalDocumentRepository documentRepository;
    private final IngestionPipeline ingestionPipeline;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    DocumentSeeder(LegalDocumentRepository documentRepository,
                   IngestionPipeline ingestionPipeline) {
        this.documentRepository = documentRepository;
        this.ingestionPipeline = ingestionPipeline;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        for (SeedDoc seed : SeedDoc.ALL) {
            if (documentRepository.existsByFilePath(seed.filename)) {
                log.info("Already ingested — skipping: {}", seed.shortName);
                continue;
            }

            if (!copyToUploadDir(seed.filename, uploadPath)) {
                continue; // classpath resource missing — skip without failing startup
            }

            LegalDocumentEntity doc = new LegalDocumentEntity(
                    seed.title, seed.shortName, seed.category, seed.versionLabel, null, seed.filename);
            doc.verify(); // mark VERIFIED so pipeline transitions it to INGESTED
            LegalDocumentEntity saved = documentRepository.save(doc);
            ingestionPipeline.ingest(saved.getId());
            log.info("Ingestion queued: {}", seed.shortName);
        }
    }

    private boolean copyToUploadDir(String filename, Path uploadPath) {
        ClassPathResource resource = new ClassPathResource("documents/" + filename);
        if (!resource.exists()) {
            log.warn("Classpath resource missing — documents/{}", filename);
            return false;
        }
        Path dest = uploadPath.resolve(filename).normalize();
        if (Files.exists(dest)) return true; // already copied from a prior run
        try {
            Files.copy(resource.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ioException) {
            log.error("Failed to copy {} to upload dir", filename, ioException);
            return false;
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
