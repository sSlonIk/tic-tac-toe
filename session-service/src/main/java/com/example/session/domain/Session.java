package com.example.session.domain;

import com.example.session.engine.dto.GameState;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Session {

    private final String id;
    private volatile SessionStatus status;
    private volatile GameStatus gameStatus;
    private final CopyOnWriteArrayList<Move> history;
    private volatile String failureReason;
    private volatile Player[] board;

    public Session(String id) {
        this.id = id;
        this.status = SessionStatus.CREATED;
        this.history = new CopyOnWriteArrayList<>();
        this.board = new Player[9];
    }

    public String getId() {
        return id;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public List<Move> getHistory() {
        return List.copyOf(history);
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Player[] getBoard() {
        return board.clone();
    }

    public synchronized void applyCreated(GameState state) {
        this.board = state.board() == null ? new Player[9] : state.board().clone();
        this.gameStatus = state.status();
        this.failureReason = null;
    }

    public synchronized boolean tryStartRunning() {
        if (this.status != SessionStatus.CREATED) {
            return false;
        }
        this.status = SessionStatus.RUNNING;
        this.failureReason = null;
        return true;
    }

    public synchronized void recordMove(Player player, int position, GameState state, Move move) {
        this.history.add(move);
        this.board = state.board() == null ? new Player[9] : state.board().clone();
        this.gameStatus = state.status();
        this.failureReason = null;
        if (state.status() != GameStatus.IN_PROGRESS) {
            this.status = SessionStatus.FINISHED;
        }
    }

    public synchronized void markFailed(String reason) {
        this.status = SessionStatus.FAILED;
        this.failureReason = reason;
    }
}
