package com.example.engine.domain;

public class WrongTurnException extends GameConflictException {

    public WrongTurnException(Player expected, Player actual) {
        super("Expected player %s but got %s".formatted(expected, actual));
    }

    @Override
    public String title() {
        return "Wrong turn";
    }
}
