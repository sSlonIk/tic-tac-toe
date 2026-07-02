package com.example.engine.store;

import com.example.engine.domain.Game;
import com.example.engine.domain.GameAlreadyExistsException;
import com.example.engine.domain.GameNotFoundException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Component;

@Component
public class GameStore {

    private final ConcurrentHashMap<String, Game> games = new ConcurrentHashMap<>();

    public Game create(String gameId) {
        Game created = new Game(gameId);
        Game existing = games.putIfAbsent(gameId, created);
        if (existing != null) {
            throw new GameAlreadyExistsException(gameId);
        }
        return created;
    }

    public Game get(String gameId) {
        Game game = games.get(gameId);
        if (game == null) {
            throw new GameNotFoundException(gameId);
        }
        return game;
    }

    public Game update(String gameId, UnaryOperator<Game> updater) {
        return games.compute(gameId, (id, game) -> {
            if (game == null) {
                throw new GameNotFoundException(gameId);
            }
            synchronized (game) {
                return updater.apply(game);
            }
        });
    }
}
