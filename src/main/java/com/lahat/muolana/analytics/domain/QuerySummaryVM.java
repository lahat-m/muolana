package com.lahat.muolana.analytics.domain;

public record QuerySummaryVM(long total, long answered, long notFound, double errorRate) {
}
