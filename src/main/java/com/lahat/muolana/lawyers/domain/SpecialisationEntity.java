package com.lahat.muolana.lawyers.domain;

import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(schema = "lawyers", name = "specialisations")
class SpecialisationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "lawyer_id", nullable = false)
    private UUID lawyerId;

    @Column(name = "area", nullable = false, length = 255)
    private String area;

    protected SpecialisationEntity() {
    }

    SpecialisationEntity(UUID lawyerId, String area) {
        this.lawyerId = lawyerId;
        this.area = area;
    }

    UUID getId() {
        return id;
    }

    UUID getLawyerId() {
        return lawyerId;
    }

    String getArea() {
        return area;
    }
}
