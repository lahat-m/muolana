package com.lahat.muolana.analytics.domain;

public record SessionSummaryVM(long total, long active, long ended, double avgMessagesPerSession) {
}
