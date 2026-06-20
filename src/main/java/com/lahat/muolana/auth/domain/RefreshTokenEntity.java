package com.lahat.muolana.auth.domain;

import com.lahat.muolana.shared.AssertUtil;
import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "auth", name = "refresh_tokens")
class RefreshTokenEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "jti", nullable = false, unique = true, length = 255)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    protected RefreshTokenEntity() {
    }

    RefreshTokenEntity(UUID userId, String jti, Instant expiresAt) {
        this.userId = AssertUtil.requireNotNull(userId, "userId required");
        this.jti = AssertUtil.requireNotBlank(jti, "jti required");
        this.expiresAt = AssertUtil.requireNotNull(expiresAt, "expiresAt required");
        this.revoked = false;
    }

    void revoke() {
        this.revoked = true;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getJti() {
        return jti;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    boolean isRevoked() {
        return revoked;
    }
}
