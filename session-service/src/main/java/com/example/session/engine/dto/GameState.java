package com.example.session.engine.dto;

import com.example.session.domain.GameStatus;
import com.example.session.domain.Player;

public record GameState(
    String gameId,
    Player[] board,
    GameStatus status,
    Player nextPlayer
) {
}
