package com.lahat.muolana.legaldocuments.domain;

import com.lahat.muolana.cloud.DocumentStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LegalDocumentService {

    private final LegalDocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final IngestionPipeline ingestionPipeline;
    private final DocumentStorageService storageService;

    public LegalDocumentService(LegalDocumentRepository documentRepository,
                                ChunkRepository chunkRepository,
                                IngestionPipeline ingestionPipeline,
                                DocumentStorageService storageService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.ingestionPipeline = ingestionPipeline;
        this.storageService = storageService;
    }

    public LegalDocumentVM upload(UploadDocumentCmd cmd) {
        LegalDocumentEntity doc = new LegalDocumentEntity(
                cmd.title(), cmd.shortName(), cmd.category(),
                cmd.versionLabel(), cmd.sourceUrl(), cmd.filePath());
        doc.verify();
        LegalDocumentEntity saved = documentRepository.save(doc);
        UUID docId = saved.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ingestionPipeline.ingest(docId);
            }
        });
        return toVM(saved);
    }

    @Transactional(readOnly = true)
    public Page<LegalDocumentVM> list(String status, String category, Pageable pageable) {
        DocumentStatus docStatus = status != null ? DocumentStatus.valueOf(status.toUpperCase()) : null;
        DocumentCategory docCategory = category != null ? DocumentCategory.valueOf(category.toUpperCase()) : null;

        Page<LegalDocumentEntity> page;
        if (docStatus != null && docCategory != null) {
            page = documentRepository.findByStatusAndCategory(docStatus, docCategory, pageable);
        } else if (docStatus != null) {
            page = documentRepository.findByStatus(docStatus, pageable);
        } else if (docCategory != null) {
            page = documentRepository.findByCategory(docCategory, pageable);
        } else {
            page = documentRepository.findAll(pageable);
        }
        return page.map(this::toVM);
    }

    @Transactional(readOnly = true)
    public long countIngested() {
        return documentRepository.countByStatusIn(
                List.of(DocumentStatus.VERIFIED, DocumentStatus.INGESTED));
    }

    @Transactional(readOnly = true)
    public LegalDocumentVM getById(UUID docId) {
        return toVM(documentRepository.getById(docId));
    }

    public LegalDocumentVM verify(UUID docId) {
        LegalDocumentEntity doc = documentRepository.getById(docId);
        doc.verify();
        documentRepository.save(doc);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ingestionPipeline.ingest(docId);
            }
        });
        return toVM(doc);
    }

    public LegalDocumentVM reject(UUID docId, String rejectionReason) {
        LegalDocumentEntity doc = documentRepository.getById(docId);
        doc.reject(rejectionReason);
        return toVM(documentRepository.save(doc));
    }

    public void delete(UUID docId) {
        LegalDocumentEntity doc = documentRepository.getById(docId);
        String objectKey = doc.getFilePath();
        documentRepository.delete(doc);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storageService.delete(objectKey);
            }
        });
    }

    @Transactional(readOnly = true)
    public Page<ChunkVM> getChunks(UUID docId, Pageable pageable) {
        documentRepository.getById(docId);
        return chunkRepository.findByDocumentId(docId, pageable).map(this::toChunkVM);
    }

    private LegalDocumentVM toVM(LegalDocumentEntity documentEntity) {
        return new LegalDocumentVM(documentEntity.getId(), documentEntity.getTitle(), documentEntity.getShortName(),
                documentEntity.getCategory().name(), documentEntity.getVersionLabel(), documentEntity.getSourceUrl(),
                documentEntity.getFilePath(), documentEntity.getStatus().name(), documentEntity.getRejectionReason(),
                documentEntity.getCreatedAt(), documentEntity.getUpdatedAt());
    }

    private ChunkVM toChunkVM(ChunkEntity chunkEntity) {
        return new ChunkVM(chunkEntity.getId(), chunkEntity.getDocumentId(), chunkEntity.getChunkIndex(), chunkEntity.getContent());
    }
}
