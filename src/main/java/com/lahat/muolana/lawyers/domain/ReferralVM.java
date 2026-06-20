package com.lahat.muolana.lawyers.domain;

import java.time.Instant;
import java.util.UUID;

public record ReferralVM(UUID id, UUID lawyerId, String status, Instant createdAt) {
}
