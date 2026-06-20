package com.lahat.muolana.auth.domain;

import com.lahat.muolana.shared.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    Page<UserEntity> findByRole(Role role, Pageable pageable);

    Page<UserEntity> findByIsActive(boolean isActive, Pageable pageable);

    Page<UserEntity> findByRoleAndIsActive(Role role, boolean isActive, Pageable pageable);

    boolean existsByEmail(String email);

    default UserEntity getByEmail(String email) {
        return findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found"));
    }

    default UserEntity getById(UUID id) {
        return findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }
}
