package com.example.engine.domain;

public abstract class GameConflictException extends RuntimeException {

    protected GameConflictException(String message) {
        super(message);
    }

    public abstract String title();
}
