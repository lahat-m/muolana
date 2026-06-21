package com.lahat.muolana.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.minio")
public record MinioProperties(
        String url,
        String accessKey,
        String secretKey,
        String bucket
) {}
