package com.lahat.muolana.conversation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    Page<MessageEntity> findBySessionIdOrderByCreatedAtAsc(UUID sessionId, Pageable pageable);

    List<MessageEntity> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    Optional<MessageEntity> findByIdAndSessionId(UUID id, UUID sessionId);
}
