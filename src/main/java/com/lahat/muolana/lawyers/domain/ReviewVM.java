package com.lahat.muolana.lawyers.domain;

import java.time.Instant;
import java.util.UUID;

public record ReviewVM(UUID id, int rating, String reviewText, boolean isAnonymous, Instant createdAt) {
}
