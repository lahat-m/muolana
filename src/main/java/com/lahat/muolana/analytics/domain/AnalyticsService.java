package com.lahat.muolana.analytics.domain;

import com.lahat.muolana.lawyers.LawyersAPI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final LawyersAPI lawyersAPI;

    public AnalyticsService(AnalyticsRepository analyticsRepository, LawyersAPI lawyersAPI) {
        this.analyticsRepository = analyticsRepository;
        this.lawyersAPI = lawyersAPI;
    }

    public Page<QueryLogVM> getQueryLogs(Instant from, Instant to, String outcome, Pageable pageable) {
        Instant resolvedFrom = from != null ? from : Instant.EPOCH;
        Instant resolvedTo = to != null ? to : Instant.now();
        if (outcome != null) {
            QueryOutcome queryOutcome = QueryOutcome.valueOf(outcome.toUpperCase());
            return analyticsRepository.findByCreatedAtBetweenAndOutcome(resolvedFrom, resolvedTo, queryOutcome, pageable)
                    .map(this::toVM);
        }
        return analyticsRepository.findByCreatedAtBetween(resolvedFrom, resolvedTo, pageable).map(this::toVM);
    }

    public QuerySummaryVM getQuerySummary(Instant from, Instant to) {
        Instant resolvedFrom = from != null ? from : Instant.EPOCH;
        Instant resolvedTo = to != null ? to : Instant.now();
        long total = analyticsRepository.countByPeriod(resolvedFrom, resolvedTo);
        long answered = analyticsRepository.countByPeriodAndOutcome(resolvedFrom, resolvedTo, QueryOutcome.ANSWERED);
        long notFound = analyticsRepository.countByPeriodAndOutcome(resolvedFrom, resolvedTo, QueryOutcome.NOT_FOUND);
        long errors = analyticsRepository.countByPeriodAndOutcome(resolvedFrom, resolvedTo, QueryOutcome.ERROR);
        double errorRate = total > 0 ? (double) errors / total : 0.0;
        return new QuerySummaryVM(total, answered, notFound, errorRate);
    }

    public List<CategoryCountVM> getTopCategories(Instant from, Instant to, int limit) {
        Instant resolvedFrom = from != null ? from : Instant.EPOCH;
        Instant resolvedTo = to != null ? to : Instant.now();
        return analyticsRepository.findTopCategories(resolvedFrom, resolvedTo, PageRequest.of(0, limit))
                .stream()
                .map(row -> new CategoryCountVM((String) row[0], (Long) row[1]))
                .toList();
    }

    public List<KnowledgeGapVM> getKnowledgeGaps(Instant from, Instant to, int limit) {
        Instant resolvedFrom = from != null ? from : Instant.EPOCH;
        Instant resolvedTo = to != null ? to : Instant.now();
        return analyticsRepository.findKnowledgeGaps(resolvedFrom, resolvedTo, PageRequest.of(0, limit))
                .stream()
                .map(row -> new KnowledgeGapVM((String) row[0], (Long) row[1]))
                .toList();
    }

    public SessionSummaryVM getSessionSummary(Instant from, Instant to) {
        Instant resolvedFrom = from != null ? from : Instant.EPOCH;
        Instant resolvedTo = to != null ? to : Instant.now();
        long total = analyticsRepository.countDistinctSessions(resolvedFrom, resolvedTo);
        Double avg = analyticsRepository.avgMessagesPerSession(resolvedFrom, resolvedTo);
        return new SessionSummaryVM(total, 0L, 0L, avg != null ? avg : 0.0);
    }

    @Transactional
    public void record(RecordQueryCmd cmd) {
        analyticsRepository.save(new QueryLogEntity(
                cmd.sessionId(), cmd.userId(), cmd.queryText(),
                cmd.outcome(), cmd.category(), cmd.responseTimeMs()));
    }

    public LawyerFunnelVM getLawyerFunnel(Instant from, Instant to) {
        return new LawyerFunnelVM(0L, 0L, 0L);
    }

    private QueryLogVM toVM(QueryLogEntity queryLogEntity) {
        return new QueryLogVM(queryLogEntity.getId(), queryLogEntity.getSessionId(), queryLogEntity.getUserId(),
                queryLogEntity.getQueryText(), queryLogEntity.getOutcome().name(), queryLogEntity.getCategory(),
                queryLogEntity.getResponseTimeMs(), queryLogEntity.getCreatedAt());
    }
}
