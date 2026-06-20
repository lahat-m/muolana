package com.lahat.muolana.legaldocuments.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ChunkRepository extends JpaRepository<ChunkEntity, UUID> {

    Page<ChunkEntity> findByDocumentId(UUID documentId, Pageable pageable);

    void deleteByDocumentId(UUID documentId);
}
