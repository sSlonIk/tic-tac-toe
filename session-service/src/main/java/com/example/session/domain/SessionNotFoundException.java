package com.example.session.domain;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String sessionId) {
        super("Session '%s' was not found".formatted(sessionId));
    }
}
