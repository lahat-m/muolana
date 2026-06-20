package com.lahat.muolana.conversation.web;

import com.lahat.muolana.conversation.domain.ConversationService;
import com.lahat.muolana.conversation.domain.UpdateSessionCmd;
import com.lahat.muolana.conversation.web.dtos.SessionResponse;
import com.lahat.muolana.conversation.web.dtos.UpdateSessionRequest;
import com.lahat.muolana.shared.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
class SessionController {

    private final ConversationService conversationService;

    SessionController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    ResponseEntity<SessionResponse> create(UriComponentsBuilder uriBuilder) {
        SessionResponse body = SessionResponse.from(conversationService.createSession());
        URI location = uriBuilder.path("/api/v1/sessions/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    PageResponse<SessionResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(conversationService.listSessions(pageable).map(SessionResponse::from));
    }

    @GetMapping("/{sessionId}")
    SessionResponse get(@PathVariable UUID sessionId) {
        return SessionResponse.from(conversationService.getSession(sessionId));
    }

    @PatchMapping("/{sessionId}")
    SessionResponse update(@PathVariable UUID sessionId,
                           @RequestBody UpdateSessionRequest updateSessionRequest) {
        return SessionResponse.from(conversationService.updateSession(sessionId,
                new UpdateSessionCmd(updateSessionRequest.title(), updateSessionRequest.status())));
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID sessionId) {
        conversationService.deleteSession(sessionId);
    }
}
