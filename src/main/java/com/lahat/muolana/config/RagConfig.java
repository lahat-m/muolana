package com.lahat.muolana.config;

import com.lahat.muolana.rag.RagPrompts;
import com.lahat.muolana.rag.RagProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
class RagConfig {

    @Value("${app.rag.hallucination-threshold:0.15}")
    private double hallucinationThreshold;

    @Value("${app.rag.top-k:10}")
    private int topK;

    @Value("classpath:prompts/rag-system.st")
    private Resource systemPromptResource;

    @Value("classpath:prompts/rag-disclaimer.st")
    private Resource disclaimerResource;

    @Bean
    RagProperties ragProperties() {
        return new RagProperties(hallucinationThreshold, topK);
    }

    @Bean
    RagPrompts ragPrompts() throws IOException {
        String system = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        String disclaimer = disclaimerResource.getContentAsString(StandardCharsets.UTF_8);
        return new RagPrompts(system.strip(), disclaimer.strip());
    }
}
