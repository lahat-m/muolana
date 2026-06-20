package com.lahat.muolana.rag;

import com.lahat.muolana.conversation.domain.MessageRole;
import com.lahat.muolana.conversation.domain.MessageVM;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class RagPipeline {

    private static final Logger log = LoggerFactory.getLogger(RagPipeline.class);

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final RagProperties ragProperties;
    private final RagPrompts ragPrompts;

    public RagPipeline(VectorStore vectorStore, ChatClient chatClient,
                       RagProperties ragProperties, RagPrompts ragPrompts) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.ragProperties = ragProperties;
        this.ragPrompts = ragPrompts;
    }

    public RagSearchResult search(String userQuery) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userQuery)
                        .topK(ragProperties.topK())
                        .similarityThreshold(0.0)
                        .build());

        String truncated = userQuery.length() > 60 ? userQuery.substring(0, 60) + "…" : userQuery;
        log.debug("RAG raw results for query \"{}\": {} docs, scores={}",
                truncated, docs.size(),
                docs.stream().map(d -> String.format("%.3f", d.getScore())).toList());

        List<ChunkMatch> matches = docs.stream()
                .map(d -> {
                    String title = (String) d.getMetadata().get("title");
                    String sourceRef = (title != null && !title.isBlank()) ? title
                            : (String) d.getMetadata().getOrDefault("short_name", "Legal Document");
                    return new ChunkMatch(
                            UUID.fromString(d.getId()),
                            d.getText(),
                            sourceRef,
                            (String) d.getMetadata().getOrDefault("category", "OTHER"),
                            d.getScore() != null ? d.getScore() : 0.0,
                            (String) d.getMetadata().getOrDefault("source_url", ""));
                })
                .filter(m -> m.score() >= ragProperties.hallucinationThreshold())
                .toList();

        log.info("RAG search: query=\"{}\" rawDocs={} aboveThreshold={}",
                truncated, docs.size(), matches.size());

        return new RagSearchResult(matches, matches.isEmpty());
    }

    public List<Message> buildMessages(List<MessageVM> history, List<ChunkMatch> chunks) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt(chunks)));
        for (MessageVM m : history) {
            if (m.role() == MessageRole.USER) {
                messages.add(new UserMessage(m.content()));
            } else if (m.role() == MessageRole.ASSISTANT) {
                messages.add(new AssistantMessage(m.content()));
            }
        }
        return messages;
    }

    /**
     * Streams LLM tokens. Used by the SSE endpoint.
     */
    public Flux<String> streamContent(List<Message> messages) {
        return chatClient.prompt()
                .messages(messages)
                .stream()
                .content();
    }

    /**
     * Structured output variant — returns a fully typed {@link LegalAnswerResponse}
     * via Spring AI's BeanOutputConverter. The converter appends JSON schema
     * instructions automatically so the model always returns valid, typed JSON.
     */
    public LegalAnswerResponse structuredAnswer(List<Message> messages) {
        return chatClient.prompt()
                .messages(messages)
                .call()
                .entity(LegalAnswerResponse.class);
    }

    public String dominantCategory(List<ChunkMatch> chunks) {
        return chunks.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ChunkMatch::category,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("OTHER");
    }

    private String buildSystemPrompt(List<ChunkMatch> chunks) {
        if (chunks.isEmpty()) return ragPrompts.systemPrompt();
        StringBuilder sb = new StringBuilder(ragPrompts.systemPrompt()).append("\n\n## Relevant legal texts\n\n");
        chunks.forEach(c -> sb.append("### ").append(c.sourceRef()).append("\n")
                .append(c.content()).append("\n\n"));
        return sb.toString();
    }
}
