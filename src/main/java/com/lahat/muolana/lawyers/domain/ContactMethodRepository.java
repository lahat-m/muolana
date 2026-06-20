package com.lahat.muolana.lawyers.domain;

import com.lahat.muolana.shared.exceptions.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ContactMethodRepository extends JpaRepository<ContactMethodEntity, UUID> {

    List<ContactMethodEntity> findByLawyerId(UUID lawyerId);

    default ContactMethodEntity getByIdAndLawyerId(UUID id, UUID lawyerId) {
        return findById(id)
                .filter(c -> c.getLawyerId().equals(lawyerId))
                .orElseThrow(() -> new ResourceNotFoundException("ContactMethod " + id + " not found"));
    }
}
