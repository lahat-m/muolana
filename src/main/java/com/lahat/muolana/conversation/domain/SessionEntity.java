package com.lahat.muolana.conversation.domain;

import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "conversations", name = "sessions")
public class SessionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = true, updatable = false)
    private UUID userId;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    protected SessionEntity() {
    }

    SessionEntity(UUID userId) {
        this.userId = userId;
        this.lastActiveAt = Instant.now();
    }

    void touch() {
        this.lastActiveAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    public SessionStatus getStatus() {
        return status;
    }

    void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }
}
