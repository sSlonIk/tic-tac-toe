package com.example.session.domain;

public class SessionConflictException extends RuntimeException {

    public SessionConflictException(String message) {
        super(message);
    }
}
