package com.example.engine.domain;

public class GameAlreadyExistsException extends GameConflictException {

    public GameAlreadyExistsException(String gameId) {
        super("Game with id '%s' already exists".formatted(gameId));
    }

    @Override
    public String title() {
        return "Game already exists";
    }
}
