package com.lahat.muolana.auth.domain;

import com.lahat.muolana.shared.AssertUtil;
import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(schema = "auth", name = "users")
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected UserEntity() {
    }

    UserEntity(String email, String passwordHash, String fullName, Role role) {
        this.email = AssertUtil.requireNotBlank(email, "email required");
        this.passwordHash = AssertUtil.requireNotNull(passwordHash, "passwordHash required");
        this.fullName = AssertUtil.requireNotBlank(fullName, "fullName required");
        this.role = AssertUtil.requireNotNull(role, "role required");
        this.isActive = true;
    }

    void deactivate() {
        this.isActive = false;
    }

    void activate() {
        this.isActive = true;
    }

    void changeRole(Role role) {
        this.role = AssertUtil.requireNotNull(role, "role required");
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return isActive;
    }
}
