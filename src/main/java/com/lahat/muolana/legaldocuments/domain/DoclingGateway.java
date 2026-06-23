package com.lahat.muolana.legaldocuments.domain;

import ai.docling.serve.api.DoclingServeApi;
import io.arconia.ai.document.docling.DoclingDocumentReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;

@Component
class DoclingGateway {

    private static final Logger log = LoggerFactory.getLogger(DoclingGateway.class);

    private final DoclingServeApi doclingServeApi;

    DoclingGateway(DoclingServeApi doclingServeApi) {
        this.doclingServeApi = doclingServeApi;
    }

    @Retryable(
            retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 20_000, multiplier = 2.0)
    )
    List<Document> chunk(ByteArrayResource resource, Map<String, Object> metadata) {
        return DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .files(resource)
                .metadata(metadata)
                .build()
                .get();
    }
}
