package com.lahat.muolana.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.http.HttpClient;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableRetry
class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean("ingestionExecutor")
    Executor ingestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ingest-");
        executor.initialize();
        return executor;
    }

    // Force HTTP/1.1 — Docling's uvicorn server does not speak HTTP/2 (h2c),
    // and Java's HttpClient attempts an upgrade by default, causing EOF errors.
    @Bean
    RestClientCustomizer forceHttp11() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return builder -> builder.requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }

    @Bean
    RetryListener retryLogger() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext ctx, RetryCallback<T, E> cb, Throwable t) {
                log.warn("Docling call failed (attempt {}), will retry — {}", ctx.getRetryCount(), t.getMessage());
            }
        };
    }
}
