package com.example.session.engine;

import com.example.session.domain.Player;
import com.example.session.engine.dto.GameState;
import com.example.session.engine.dto.MoveRequest;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class EngineClient {

  private static final Logger log = LoggerFactory.getLogger(EngineClient.class);

  private final RestClient client;

  public EngineClient(RestClient.Builder restClientBuilder, @Value("${engine.base-url}") String baseUrl) {
    this.client = restClientBuilder.baseUrl(baseUrl).build();
  }

  public GameState createGame(String gameId) {
    return execute(() -> client.post()
        .uri("/games/{gameId}", gameId)
        .retrieve()
        .body(GameState.class), 3);
  }

  public GameState move(String gameId, Player player, int position) {
    return execute(() -> client.post()
        .uri("/games/{gameId}/move", gameId)
        .body(new MoveRequest(player, position))
        .retrieve()
        .body(GameState.class), 1);
  }

  private <T> T execute(Supplier<T> call, int maxAttempts) {
    ResourceAccessException lastConnectionError = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return call.get();
      } catch (ResourceAccessException exception) {
        lastConnectionError = exception;
        if (attempt < maxAttempts) {
          log.warn("Engine connection failed (attempt {}/{}): {}", attempt, maxAttempts, exception.getMessage());
        }
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
