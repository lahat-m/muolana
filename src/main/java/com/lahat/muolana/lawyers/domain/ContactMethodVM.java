package com.lahat.muolana.lawyers.domain;

import java.util.UUID;

public record ContactMethodVM(UUID id, String channel, String value, boolean isPrimary) {
}
