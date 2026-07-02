package com.example.session.simulation;

import com.example.session.domain.GameStatus;
import com.example.session.domain.Move;
import com.example.session.domain.Player;
import com.example.session.domain.Session;
import com.example.session.domain.SessionStore;
import com.example.session.api.SessionEventPublisher;
import com.example.session.engine.EngineClient;
import com.example.session.engine.dto.GameState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final SessionStore sessionStore;
    private final EngineClient engineClient;
    private final SessionEventPublisher eventPublisher;
    private final long moveDelayMs;

    public SimulationService(
        SessionStore sessionStore,
        EngineClient engineClient,
        SessionEventPublisher eventPublisher,
        @Value("${simulation.move-delay-ms}") long moveDelayMs
    ) {
        this.sessionStore = sessionStore;
        this.engineClient = engineClient;
        this.eventPublisher = eventPublisher;
        this.moveDelayMs = moveDelayMs;
    }

    @Async
    public void simulate(String sessionId) {
        Session session = sessionStore.get(sessionId);
        Player[] board = session.getBoard();
        Player player = Player.X;
        try {
            while (true) {
                int position = pickRandomFreeCell(board);
                GameState state = engineClient.move(sessionId, player, position);
                Move move = new Move(session.getHistory().size() + 1, player, position, Instant.now());
                session.recordMove(player, position, state, move);
                eventPublisher.publish(session);
                board = state.board() == null ? new Player[9] : state.board().clone();
                if (state.status() != GameStatus.IN_PROGRESS) {
                    return;
                }
                player = state.nextPlayer();
                Thread.sleep(moveDelayMs);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            session.markFailed("Simulation interrupted");
            eventPublisher.publish(session);
            log.error("Simulation interrupted for session {}", sessionId);
        } catch (Exception exception) {
            session.markFailed(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
            eventPublisher.publish(session);
            log.error("Simulation failed for session {}", sessionId, exception);
        }
    }

    private int pickRandomFreeCell(Player[] board) {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                free.add(i);
            }
        }
        if (free.isEmpty()) {
            throw new IllegalStateException("No free cells left");
        }
        return free.get(ThreadLocalRandom.current().nextInt(free.size()));
    }
}
