package com.example.engine.api;

import com.example.engine.api.dto.GameStateResponse;
import com.example.engine.api.dto.MoveRequest;
import com.example.engine.domain.GameLogic;
import com.example.engine.store.GameStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games")
public class GameController {

  private final GameStore gameStore;
  private final GameLogic gameLogic = new GameLogic();

  public GameController(GameStore gameStore) {
    this.gameStore = gameStore;
  }

  @PostMapping("/{gameId}")
  @ResponseStatus(HttpStatus.CREATED)
  public GameStateResponse create(@PathVariable("gameId") String gameId) {
    return GameStateResponse.from(gameStore.create(gameId));
  }

  @PostMapping("/{gameId}/move")
  public GameStateResponse move(
      @PathVariable("gameId") String gameId,
      @Valid @RequestBody MoveRequest request
  ) {
    return gameStore.withGame(gameId, game -> {
      gameLogic.applyMove(game, request.player(), request.position());
      return GameStateResponse.from(game);
    });
  }

  @GetMapping("/{gameId}")
  public GameStateResponse get(@PathVariable("gameId") String gameId) {
    return gameStore.withGame(gameId, GameStateResponse::from);
  }
}
