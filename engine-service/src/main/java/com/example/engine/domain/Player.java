package com.example.engine.domain;

public enum Player {
    X,
    O;

    public Player opposite() {
        return this == X ? O : X;
    }
}
