package com.lahat.muolana.conversation.web.dtos;

import com.lahat.muolana.conversation.domain.MessageVM;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, UUID sessionId, String role, String content,
                              UUID[] lawChunksUsed, Double[] cosineScores,
                              boolean guardTriggered, String model,
                              Integer inputTokens, Integer outputTokens, Instant createdAt) {

    public static MessageResponse from(MessageVM vm) {
        return new MessageResponse(
                vm.id(), vm.sessionId(), vm.role().name(), vm.content(),
                vm.lawChunksUsed(), vm.cosineScores(), vm.guardTriggered(),
                vm.model(), vm.inputTokens(), vm.outputTokens(), vm.createdAt());
    }
}
