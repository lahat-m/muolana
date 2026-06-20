package com.lahat.muolana.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 3;
    private static final long WINDOW_MILLIS = 30_000L;

    private final Map<String, Deque<Long>> timestamps = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        long now = Instant.now().toEpochMilli();
        Deque<Long> requestTimestamps = timestamps.computeIfAbsent(clientIp, key -> new ConcurrentLinkedDeque<>());
        synchronized (requestTimestamps) {
            long windowStart = now - WINDOW_MILLIS;
            while (!requestTimestamps.isEmpty() && requestTimestamps.peekFirst() < windowStart) requestTimestamps.pollFirst();
            if (requestTimestamps.size() >= MAX_REQUESTS) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
                return;
            }
            requestTimestamps.addLast(now);
        }
        chain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
