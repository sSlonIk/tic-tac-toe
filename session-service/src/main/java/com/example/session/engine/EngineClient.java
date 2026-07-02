package com.example.session.engine;

import com.example.session.domain.Player;
import com.example.session.engine.dto.GameState;
import com.example.session.engine.dto.MoveRequest;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class EngineClient {

  private final RestClient client;

  public EngineClient(RestClient.Builder restClientBuilder, @Value("${engine.base-url}") String baseUrl) {
    this.client = restClientBuilder.baseUrl(baseUrl).build();
  }

  public GameState createGame(String gameId) {
    return executeWithRetry(() -> client.post()
        .uri("/games/{gameId}", gameId)
        .retrieve()
        .body(GameState.class));
  }

  public GameState move(String gameId, Player player, int position) {
    return executeWithRetry(() -> client.post()
        .uri("/games/{gameId}/move", gameId)
        .body(new MoveRequest(player, position))
        .retrieve()
        .body(GameState.class));
  }

  private <T> T executeWithRetry(Supplier<T> call) {
    ResourceAccessException lastConnectionError = null;
    for (int attempt = 0; attempt < 3; attempt++) {
      try {
        return call.get();
      } catch (ResourceAccessException exception) {
        lastConnectionError = exception;
      } catch (RestClientResponseException exception) {
        throw new EngineUnavailableException(
            "Engine responded with %d: %s".formatted(
                exception.getStatusCode().value(),
                exception.getResponseBodyAsString()
            )
        );
      } catch (RestClientException exception) {
        throw new EngineUnavailableException("Engine request failed: %s".formatted(exception.getMessage()));
      }
    }
    throw new EngineUnavailableException(
        "Engine is unavailable: %s".formatted(
            lastConnectionError == null ? "connection failed" : lastConnectionError.getMessage()
        )
    );
  }
}