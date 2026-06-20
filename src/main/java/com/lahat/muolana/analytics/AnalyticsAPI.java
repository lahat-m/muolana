package com.lahat.muolana.analytics;

import com.lahat.muolana.analytics.domain.AnalyticsService;
import com.lahat.muolana.analytics.domain.QuerySummaryVM;
import com.lahat.muolana.analytics.domain.RecordQueryCmd;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsAPI {

    private final AnalyticsService analyticsService;

    public AnalyticsAPI(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public void recordQuery(RecordQueryCmd cmd) {
        analyticsService.record(cmd);
    }

    public QuerySummaryVM allTimeSummary() {
        return analyticsService.getQuerySummary(null, null);
    }
}
