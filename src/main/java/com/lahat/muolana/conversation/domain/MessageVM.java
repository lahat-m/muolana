package com.lahat.muolana.conversation.domain;

import java.time.Instant;
import java.util.UUID;

public record MessageVM(UUID id, UUID sessionId, MessageRole role, String content,
                        UUID[] lawChunksUsed, Double[] cosineScores,
                        boolean guardTriggered, String model,
                        Integer inputTokens, Integer outputTokens, Instant createdAt) {

    static MessageVM from(MessageEntity messageEntity) {
        return new MessageVM(
                messageEntity.getId(),
                messageEntity.getSession().getId(),
                messageEntity.getRole(),
                messageEntity.getContent(),
                messageEntity.getLawChunksUsed(),
                messageEntity.getCosineScores(),
                messageEntity.isGuardTriggered(),
                messageEntity.getModel(),
                messageEntity.getInputTokens(),
                messageEntity.getOutputTokens(),
                messageEntity.getCreatedAt());
    }
}
