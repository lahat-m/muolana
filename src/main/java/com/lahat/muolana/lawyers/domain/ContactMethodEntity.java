package com.lahat.muolana.lawyers.domain;

import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(schema = "lawyers", name = "contact_methods")
class ContactMethodEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "lawyer_id", nullable = false)
    private UUID lawyerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private ContactChannel channel;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    protected ContactMethodEntity() {
    }

    ContactMethodEntity(UUID lawyerId, ContactChannel channel, String value, boolean isPrimary) {
        this.lawyerId = lawyerId;
        this.channel = channel;
        this.value = value;
        this.isPrimary = isPrimary;
    }

    UUID getId() {
        return id;
    }

    UUID getLawyerId() {
        return lawyerId;
    }

    ContactChannel getChannel() {
        return channel;
    }

    String getValue() {
        return value;
    }

    boolean isPrimary() {
        return isPrimary;
    }
}
