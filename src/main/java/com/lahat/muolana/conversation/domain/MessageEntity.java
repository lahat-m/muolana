package com.lahat.muolana.conversation.domain;

import com.lahat.muolana.shared.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(schema = "conversations", name = "messages")
public class MessageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private SessionEntity session;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "law_chunks_used", columnDefinition = "uuid[]")
    private UUID[] lawChunksUsed = new UUID[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "cosine_scores", columnDefinition = "float8[]")
    private Double[] cosineScores = new Double[0];

    @Column(name = "guard_triggered", nullable = false)
    private boolean guardTriggered = false;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    protected MessageEntity() {
    }

    MessageEntity(SessionEntity session, MessageRole role, String content) {
        this.session = session;
        this.role = role;
        this.content = content;
    }

    void setRagMetadata(UUID[] chunks, Double[] scores, boolean guardTriggered,
                        String model, Integer inputTokens, Integer outputTokens) {
        this.lawChunksUsed = chunks != null ? chunks : new UUID[0];
        this.cosineScores = scores != null ? scores : new Double[0];
        this.guardTriggered = guardTriggered;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public UUID getId() {
        return id;
    }

    public SessionEntity getSession() {
        return session;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public UUID[] getLawChunksUsed() {
        return lawChunksUsed;
    }

    public Double[] getCosineScores() {
        return cosineScores;
    }

    public boolean isGuardTriggered() {
        return guardTriggered;
    }

    public String getModel() {
        return model;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }
}
