package com.lahat.muolana.lawyers.domain;

import com.lahat.muolana.shared.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface LawyerRepository extends JpaRepository<LawyerEntity, UUID> {

    Optional<LawyerEntity> findByBarNumber(String barNumber);

    Page<LawyerEntity> findByStatus(LawyerStatus status, Pageable pageable);

    Page<LawyerEntity> findByStatusAndLocationCityContainingIgnoreCase(LawyerStatus status, String city, Pageable pageable);

    boolean existsByBarNumber(String barNumber);

    default LawyerEntity getById(UUID id) {
        return findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer " + id + " not found"));
    }
}
