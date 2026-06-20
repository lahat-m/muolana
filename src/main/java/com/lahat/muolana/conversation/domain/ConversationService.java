package com.lahat.muolana.conversation.domain;

import com.lahat.muolana.auth.config.SecurityService;
import com.lahat.muolana.shared.exceptions.ResourceNotFoundException;
import com.lahat.muolana.shared.exceptions.UnprocessableEntityException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ConversationService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final SecurityService securityService;

    public ConversationService(SessionRepository sessionRepository,
                               MessageRepository messageRepository,
                               SecurityService securityService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.securityService = securityService;
    }

    public SessionVM createSession() {
        UUID userId = securityService.isAuthenticated() ? securityService.getCurrentUserId().id() : null;
        SessionEntity session = new SessionEntity(userId);
        return SessionVM.from(sessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public Page<SessionVM> listSessions(Pageable pageable) {
        if (!securityService.isAuthenticated()) return Page.empty(pageable);
        if (securityService.isAdmin()) {
            return sessionRepository.findAll(pageable).map(SessionVM::from);
        }
        return sessionRepository.findByUserId(securityService.getCurrentUserId().id(), pageable)
                .map(SessionVM::from);
    }

    @Transactional(readOnly = true)
    public SessionVM getSession(UUID sessionId) {
        return SessionVM.from(requireAccess(sessionId));
    }

    public SessionVM updateSession(UUID sessionId, UpdateSessionCmd cmd) {
        SessionEntity session = requireAccess(sessionId);
        if (cmd.title() != null) session.setTitle(cmd.title());
        if (cmd.status() != null) session.setStatus(cmd.status());
        return SessionVM.from(session);
    }

    public void deleteSession(UUID sessionId) {
        sessionRepository.delete(requireAccess(sessionId));
    }

    public MessageVM createMessage(UUID sessionId, String content) {
        SessionEntity session = requireAccess(sessionId);
        if (session.getStatus() == SessionStatus.ENDED) {
            throw new UnprocessableEntityException("Cannot add messages to an ended session");
        }
        if (session.getTitle() == null) {
            session.setTitle(content.length() > 60 ? content.substring(0, 60) + "…" : content);
        }
        session.touch();
        return MessageVM.from(messageRepository.save(new MessageEntity(session, MessageRole.USER, content)));
    }

    @Transactional(readOnly = true)
    public Page<MessageVM> listMessages(UUID sessionId, Pageable pageable) {
        requireAccess(sessionId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId, pageable)
                .map(MessageVM::from);
    }

    @Transactional(readOnly = true)
    public MessageVM getMessage(UUID sessionId, UUID messageId) {
        requireAccess(sessionId);
        return messageRepository.findByIdAndSessionId(messageId, sessionId)
                .map(MessageVM::from)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    }

    @Transactional(readOnly = true)
    public List<MessageVM> getHistory(UUID sessionId) {
        requireAccess(sessionId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(MessageVM::from).toList();
    }

    public MessageVM saveAssistantMessage(UUID sessionId, String content,
                                          UUID[] chunks, Double[] scores,
                                          boolean guardTriggered, String model,
                                          Integer inputTokens, Integer outputTokens) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        MessageEntity msg = new MessageEntity(session, MessageRole.ASSISTANT, content);
        msg.setRagMetadata(chunks, scores, guardTriggered, model, inputTokens, outputTokens);
        return MessageVM.from(messageRepository.save(msg));
    }

    @Transactional(readOnly = true)
    public boolean sessionExists(UUID sessionId) {
        return sessionRepository.existsById(sessionId);
    }

    private SessionEntity requireAccess(UUID sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        // Anonymous sessions are accessible by session ID alone (UUID v4 is unguessable)
        if (session.getUserId() == null) return session;
        // Authenticated sessions: caller must own them or be admin
        UUID currentUserId = securityService.isAuthenticated()
                ? securityService.getCurrentUserId().id() : null;
        if (!securityService.isAdmin() && !session.getUserId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Session not found");
        }
        return session;
    }
}
