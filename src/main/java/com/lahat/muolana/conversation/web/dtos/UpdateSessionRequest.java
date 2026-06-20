package com.lahat.muolana.conversation.web.dtos;

import com.lahat.muolana.conversation.domain.SessionStatus;

public record UpdateSessionRequest(String title, SessionStatus status) {
}
