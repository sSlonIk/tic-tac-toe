package com.example.engine.domain;

public class Game {

    private final String id;
    private final Player[] board;
    private GameStatus status;
    private Player nextPlayer;

    public Game(String id) {
        this.id = id;
        this.board = new Player[9];
        this.status = GameStatus.IN_PROGRESS;
        this.nextPlayer = Player.X;
    }

    public String getId() {
        return id;
    }

    public Player[] getBoard() {
        return board;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public Player getNextPlayer() {
        return nextPlayer;
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
    }
}
