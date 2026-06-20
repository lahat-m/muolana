package com.lahat.muolana.lawyers.domain;

import com.lahat.muolana.shared.exceptions.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpecialisationRepository extends JpaRepository<SpecialisationEntity, UUID> {

    List<SpecialisationEntity> findByLawyerId(UUID lawyerId);

    default SpecialisationEntity getByIdAndLawyerId(UUID id, UUID lawyerId) {
        return findById(id)
                .filter(s -> s.getLawyerId().equals(lawyerId))
                .orElseThrow(() -> new ResourceNotFoundException("Specialisation " + id + " not found"));
    }
}
