package com.lahat.muolana.lawyers.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ReferralRepository extends JpaRepository<ReferralEntity, UUID> {

    List<ReferralEntity> findBySessionId(UUID sessionId);

    long countByLawyerId(UUID lawyerId);
}
