package com.example.engine.domain;

public class GameFinishedException extends GameConflictException {

    public GameFinishedException(GameStatus status) {
        super("Game is already finished with status %s".formatted(status));
    }

    @Override
    public String title() {
        return "Game finished";
    }
}
