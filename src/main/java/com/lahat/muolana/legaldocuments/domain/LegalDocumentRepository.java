package com.lahat.muolana.legaldocuments.domain;

import com.lahat.muolana.shared.exceptions.ResourceNotFoundException;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

interface LegalDocumentRepository extends JpaRepository<LegalDocumentEntity, UUID> {

    Page<LegalDocumentEntity> findByStatus(DocumentStatus status, Pageable pageable);

    Page<LegalDocumentEntity> findByCategory(DocumentCategory category, Pageable pageable);

    Page<LegalDocumentEntity> findByStatusAndCategory(DocumentStatus status, DocumentCategory category, Pageable pageable);

    long countByStatus(DocumentStatus status);

    long countByStatusIn(Collection<DocumentStatus> statuses);

    boolean existsByFilePath(String filePath);

    boolean existsByShortName(String shortName);

    java.util.Optional<LegalDocumentEntity> findByShortName(String shortName);

    default @NonNull LegalDocumentEntity getById(@NonNull UUID id) {
        return findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Legal document " + id + " not found"));
    }
}
