package com.lahat.muolana.lawyers.domain;

import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(schema = "lawyers", name = "referrals")
class ReferralEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "lawyer_id", nullable = false)
    private UUID lawyerId;

    @Column(name = "citizen_user_id", nullable = false)
    private UUID citizenUserId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private ContactChannel channel;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;

    @Column(name = "status", nullable = false, length = 20)
    private final String status = "SENT";

    protected ReferralEntity() {
    }

    ReferralEntity(UUID lawyerId, UUID citizenUserId, UUID sessionId, ContactChannel channel, boolean consentGiven) {
        this.lawyerId = lawyerId;
        this.citizenUserId = citizenUserId;
        this.sessionId = sessionId;
        this.channel = channel;
        this.consentGiven = consentGiven;
    }

    UUID getId() {
        return id;
    }

    UUID getLawyerId() {
        return lawyerId;
    }

    UUID getCitizenUserId() {
        return citizenUserId;
    }

    UUID getSessionId() {
        return sessionId;
    }

    ContactChannel getChannel() {
        return channel;
    }

    boolean isConsentGiven() {
        return consentGiven;
    }

    String getStatus() {
        return status;
    }
}
