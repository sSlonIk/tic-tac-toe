package com.example.engine.domain;

public class CellOccupiedException extends GameConflictException {

    public CellOccupiedException(int position, Player occupant) {
        super("Cell %d is already taken by %s".formatted(position, occupant));
    }

    @Override
    public String title() {
        return "Cell occupied";
    }
}
