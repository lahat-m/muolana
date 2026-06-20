package com.lahat.muolana.lawyers.domain;

import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(schema = "lawyers", name = "reviews")
class ReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "lawyer_id", nullable = false)
    private UUID lawyerId;

    @Column(name = "citizen_user_id", nullable = false)
    private UUID citizenUserId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;

    protected ReviewEntity() {
    }

    ReviewEntity(UUID lawyerId, UUID citizenUserId, UUID sessionId, int rating, String reviewText, boolean isAnonymous) {
        this.lawyerId = lawyerId;
        this.citizenUserId = citizenUserId;
        this.sessionId = sessionId;
        this.rating = rating;
        this.reviewText = reviewText;
        this.isAnonymous = isAnonymous;
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

    int getRating() {
        return rating;
    }

    String getReviewText() {
        return reviewText;
    }

    boolean isAnonymous() {
        return isAnonymous;
    }
}
