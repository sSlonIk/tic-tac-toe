package com.example.session.api.dto;

import com.example.session.domain.GameStatus;
import com.example.session.domain.Move;
import com.example.session.domain.Player;
import com.example.session.domain.Session;
import com.example.session.domain.SessionStatus;
import java.util.List;

public record SessionStateResponse(
    String sessionId,
    SessionStatus status,
    GameStatus gameStatus,
    Player[] board,
    List<Move> history,
    String failureReason
) {
    public static SessionStateResponse from(Session session) {
        return new SessionStateResponse(
            session.getId(),
            session.getStatus(),
            session.getGameStatus(),
            session.getBoard(),
            session.getHistory(),
            session.getFailureReason()
        );
    }
}
