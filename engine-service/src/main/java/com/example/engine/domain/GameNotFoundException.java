package com.example.engine.domain;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(String gameId) {
        super("Game '%s' was not found".formatted(gameId));
    }
}
