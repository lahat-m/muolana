package com.lahat.muolana.conversation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    Page<SessionEntity> findByUserId(UUID userId, Pageable pageable);
}
