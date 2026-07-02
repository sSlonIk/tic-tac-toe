package com.example.engine.api.dto;

import com.example.engine.domain.Game;
import com.example.engine.domain.GameStatus;
import com.example.engine.domain.Player;

public record GameStateResponse(
    String gameId,
    Player[] board,
    GameStatus status,
    Player nextPlayer
) {
    public static GameStateResponse from(Game game) {
        return new GameStateResponse(
            game.getId(),
            game.getBoard().clone(),
            game.getStatus(),
            game.getNextPlayer()
        );
    }
}
