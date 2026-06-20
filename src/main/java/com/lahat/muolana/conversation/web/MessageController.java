package com.lahat.muolana.conversation.web;

import com.lahat.muolana.analytics.AnalyticsAPI;
import com.lahat.muolana.analytics.domain.QueryOutcome;
import com.lahat.muolana.analytics.domain.RecordQueryCmd;
import com.lahat.muolana.conversation.domain.ConversationService;
import com.lahat.muolana.conversation.domain.MessageVM;
import com.lahat.muolana.conversation.web.dtos.CreateMessageRequest;
import com.lahat.muolana.conversation.web.dtos.MessageResponse;
import com.lahat.muolana.rag.ChunkMatch;
import com.lahat.muolana.rag.LegalAnswerResponse;
import com.lahat.muolana.rag.RagPipeline;
import com.lahat.muolana.rag.RagSearchResult;
import com.lahat.muolana.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/messages")
class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final ConversationService conversationService;
    private final RagPipeline ragPipeline;
    private final AnalyticsAPI analyticsAPI;
    private final String modelName;

    MessageController(ConversationService conversationService,
                      RagPipeline ragPipeline,
                      AnalyticsAPI analyticsAPI,
                      @Value("${app.ai.model-name:unknown}") String modelName) {
        this.conversationService = conversationService;
        this.ragPipeline = ragPipeline;
        this.analyticsAPI = analyticsAPI;
        this.modelName = modelName;
    }

    private static String truncate(String text) {
        return text != null && text.length() > 120 ? text.substring(0, 120) + "…" : text;
    }

    @PostMapping
    ResponseEntity<MessageResponse> submit(@PathVariable UUID sessionId,
                                           @RequestBody @Valid CreateMessageRequest req,
                                           UriComponentsBuilder uriBuilder) {
        log.info("MSG_SUBMIT session={} len={} query=\"{}\"",
                sessionId, req.content().length(), truncate(req.content()));
        MessageResponse body = MessageResponse.from(conversationService.createMessage(sessionId, req.content()));
        log.info("MSG_CREATED session={} messageId={}", sessionId, body.id());
        URI location = uriBuilder.path("/api/v1/sessions/{sid}/messages/{mid}")
                .buildAndExpand(sessionId, body.id()).toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(body);
    }

    @GetMapping
    PageResponse<MessageResponse> list(@PathVariable UUID sessionId,
                                       @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.of(conversationService.listMessages(sessionId, pageable).map(MessageResponse::from));
    }

    @GetMapping("/{messageId}")
    MessageResponse get(@PathVariable UUID sessionId, @PathVariable UUID messageId) {
        return MessageResponse.from(conversationService.getMessage(sessionId, messageId));
    }

    /**
     * SSE streaming endpoint — streams LLM tokens as server-sent events and persists
     * the assistant message + query log on completion.
     */
    @GetMapping(value = "/{messageId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@PathVariable UUID sessionId, @PathVariable UUID messageId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        long startTime = System.currentTimeMillis();

        List<MessageVM> history = conversationService.getHistory(sessionId);
        String userQuery = lastUserMessage(history);

        RagSearchResult ragResult;
        try {
            ragResult = ragPipeline.search(userQuery);
        } catch (Exception exception) {
            log.error("RAG search failed", exception);
            sendAndComplete(emitter, Map.of("type", "error",
                    "message", "Service temporarily unavailable. Please try again."));
            return emitter;
        }
        String category = ragPipeline.dominantCategory(ragResult.chunks());

        log.info("STREAM_START session={} msgId={} chunks={} guard={} query=\"{}\"",
                sessionId, messageId, ragResult.chunks().size(), ragResult.guardTriggered(), truncate(userQuery));

        if (ragResult.guardTriggered()) {
            log.info("STREAM_NOT_FOUND session={} elapsed={}ms query=\"{}\"",
                    sessionId, elapsed(startTime), truncate(userQuery));
            sendAndComplete(emitter, Map.of("type", "not_found",
                    "message", "No relevant legal text found for this question."));
            analyticsAPI.recordQuery(new RecordQueryCmd(
                    sessionId, null, userQuery, QueryOutcome.NOT_FOUND,
                    category, elapsed(startTime)));
            return emitter;
        }

        ragResult.chunks().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ChunkMatch::sourceRef, chunk -> chunk, (existing, ignored) -> existing,
                        java.util.LinkedHashMap::new))
                .values()
                .forEach(chunk -> {
                    Map<String, Object> citationEvent = new HashMap<>();
                    citationEvent.put("type", "citation");
                    citationEvent.put("ref", chunk.sourceRef());
                    if (chunk.sourceUrl() != null && !chunk.sourceUrl().isEmpty()) {
                        citationEvent.put("url", chunk.sourceUrl());
                    }
                    sendQuietly(emitter, citationEvent);
                });

        StringBuilder accumulated = new StringBuilder();
        List<Message> messages = ragPipeline.buildMessages(history, ragResult.chunks());

        ragPipeline.streamContent(messages)
                .subscribe(
                        chunk -> {
                            try {
                                accumulated.append(chunk);
                                emitter.send(SseEmitter.event()
                                        .data(Map.of("type", "chunk", "content", chunk)));
                            } catch (IOException ioException) {
                                emitter.completeWithError(ioException);
                            }
                        },
                        error -> {
                            log.warn("STREAM_ERROR session={} msgId={} elapsed={}ms error=\"{}\"",
                                    sessionId, messageId, elapsed(startTime), error.getMessage());
                            sendQuietly(emitter, Map.of("type", "error",
                                    "message", error.getMessage() != null ? error.getMessage() : "stream error"));
                            analyticsAPI.recordQuery(new RecordQueryCmd(
                                    sessionId, null, userQuery, QueryOutcome.ERROR,
                                    category, elapsed(startTime)));
                            emitter.completeWithError(error);
                        },
                        () -> {
                            try {
                                UUID[] chunkIds = ragResult.chunks().stream()
                                        .map(ChunkMatch::chunkId).toArray(UUID[]::new);
                                Double[] scores = ragResult.chunks().stream()
                                        .map(ChunkMatch::score).toArray(Double[]::new);

                                conversationService.saveAssistantMessage(
                                        sessionId, accumulated.toString(),
                                        chunkIds, scores, false,
                                        modelName, null, null);

                                analyticsAPI.recordQuery(new RecordQueryCmd(
                                        sessionId, null, userQuery, QueryOutcome.ANSWERED,
                                        category, elapsed(startTime)));

                                log.info("STREAM_DONE session={} msgId={} elapsed={}ms answer_chars={} chunks={}",
                                        sessionId, messageId, elapsed(startTime),
                                        accumulated.length(), ragResult.chunks().size());

                                emitter.send(SseEmitter.event()
                                        .data(Map.of("type", "done", "guardTriggered", false)));
                                emitter.complete();
                            } catch (IOException ioException) {
                                emitter.completeWithError(ioException);
                            }
                        });

        return emitter;
    }

    /**
     * Structured output endpoint — synchronous alternative to SSE streaming.
     * Returns a fully typed {@link LegalAnswerResponse} with citations and
     * category using Spring AI's BeanOutputConverter via {@code entity()}.
     */
    @GetMapping("/{messageId}/answer")
    LegalAnswerResponse answer(@PathVariable UUID sessionId, @PathVariable UUID messageId) {
        List<MessageVM> history = conversationService.getHistory(sessionId);
        String userQuery = lastUserMessage(history);
        RagSearchResult ragResult = ragPipeline.search(userQuery);
        List<Message> messages = ragPipeline.buildMessages(history, ragResult.chunks());
        return ragPipeline.structuredAnswer(messages);
    }

    private String lastUserMessage(List<MessageVM> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).role().name().equals("USER")) return history.get(i).content();
        }
        return "";
    }

    private long elapsed(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private void sendAndComplete(SseEmitter emitter, Object data) {
        try {
            emitter.send(SseEmitter.event().data(data));
            emitter.complete();
        } catch (IOException ioException) {
            emitter.completeWithError(ioException);
        }
    }

    private void sendQuietly(SseEmitter emitter, Object data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException ignored) {
        }
    }
}
