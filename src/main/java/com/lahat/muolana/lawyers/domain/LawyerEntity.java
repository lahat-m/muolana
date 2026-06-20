package com.lahat.muolana.lawyers.domain;

import com.lahat.muolana.shared.AssertUtil;
import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(schema = "lawyers", name = "lawyers")
class LawyerEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "bar_number", nullable = false, unique = true, length = 100)
    private String barNumber;

    @Column(name = "location_city", length = 255)
    private String locationCity;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    private FeeType feeType;

    @Column(name = "law_firm_name", length = 255)
    private String lawFirmName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LawyerStatus status = LawyerStatus.PENDING;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    protected LawyerEntity() {
    }

    LawyerEntity(String fullName, String barNumber, String locationCity, FeeType feeType) {
        this.fullName = AssertUtil.requireNotBlank(fullName, "fullName required");
        this.barNumber = AssertUtil.requireNotBlank(barNumber, "barNumber required");
        this.locationCity = locationCity;
        this.feeType = AssertUtil.requireNotNull(feeType, "feeType required");
    }

    void approve() {
        this.status = LawyerStatus.ACTIVE;
        this.isVerified = true;
    }

    void suspend() {
        this.status = LawyerStatus.SUSPENDED;
    }

    void updateProfile(String fullName, String locationCity, FeeType feeType, String bio, String lawFirmName) {
        if (fullName != null) this.fullName = fullName;
        if (locationCity != null) this.locationCity = locationCity;
        if (feeType != null) this.feeType = feeType;
        if (bio != null) this.bio = bio;
        if (lawFirmName != null) this.lawFirmName = lawFirmName;
    }

    void addReview(int rating) {
        int totalRating = this.averageRating.multiply(BigDecimal.valueOf(this.reviewCount)).intValue() + rating;
        this.reviewCount++;
        this.averageRating = BigDecimal.valueOf(totalRating).divide(BigDecimal.valueOf(this.reviewCount), 2, java.math.RoundingMode.HALF_UP);
    }

    UUID getId() {
        return id;
    }

    String getFullName() {
        return fullName;
    }

    String getBarNumber() {
        return barNumber;
    }

    String getLawFirmName() {
        return lawFirmName;
    }

    String getLocationCity() {
        return locationCity;
    }

    FeeType getFeeType() {
        return feeType;
    }

    String getBio() {
        return bio;
    }

    boolean isVerified() {
        return isVerified;
    }

    LawyerStatus getStatus() {
        return status;
    }

    BigDecimal getAverageRating() {
        return averageRating;
    }

    int getReviewCount() {
        return reviewCount;
    }
}
