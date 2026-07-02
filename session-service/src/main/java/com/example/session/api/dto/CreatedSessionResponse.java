package com.example.session.api.dto;

import com.example.session.domain.SessionStatus;

public record CreatedSessionResponse(String sessionId, SessionStatus status) {
}
