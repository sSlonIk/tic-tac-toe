package com.example.engine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.engine.api.dto.GameStateResponse;
import com.example.engine.api.dto.MoveRequest;
import com.example.engine.domain.GameStatus;
import com.example.engine.domain.Player;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createMoveGetHappyPath() {
        String gameId = "happy-game";

        ResponseEntity<GameStateResponse> created = restTemplate.postForEntity(url("/games/" + gameId), null, GameStateResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getBody());
        assertEquals(GameStatus.IN_PROGRESS, created.getBody().status());
        assertEquals(Player.X, created.getBody().nextPlayer());

        ResponseEntity<GameStateResponse> moved = restTemplate.postForEntity(
            url("/games/" + gameId + "/move"),
            new MoveRequest(Player.X, 0),
            GameStateResponse.class
        );
        assertEquals(HttpStatus.OK, moved.getStatusCode());
        assertNotNull(moved.getBody());
        assertEquals(Player.X, moved.getBody().board()[0]);
        assertEquals(Player.O, moved.getBody().nextPlayer());

        ResponseEntity<GameStateResponse> fetched = restTemplate.getForEntity(url("/games/" + gameId), GameStateResponse.class);
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertNotNull(fetched.getBody());
        assertEquals(Player.X, fetched.getBody().board()[0]);
    }

    @Test
    void shouldReturn404ForMissingGame() {
        ResponseEntity<Map<String, Object>> response = exchangeForMap(HttpMethod.GET, "/games/missing", null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
    }

    @Test
    void shouldReturn409WithProblemDetailForOccupiedCell() {
        String gameId = "occupied-game";
        restTemplate.postForEntity(url("/games/" + gameId), null, GameStateResponse.class);
        restTemplate.postForEntity(url("/games/" + gameId + "/move"), new MoveRequest(Player.X, 0), GameStateResponse.class);

        ResponseEntity<Map<String, Object>> response = exchangeForMap(
            HttpMethod.POST,
            "/games/" + gameId + "/move",
            new MoveRequest(Player.O, 0)
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Cell occupied", response.getBody().get("title"));
        assertEquals("about:blank", response.getBody().get("type"));
        assertEquals("/games/" + gameId + "/move", response.getBody().get("instance"));
    }

    @Test
    void shouldReturn400ForOutOfRangePosition() {
        String gameId = "bad-position";
        restTemplate.postForEntity(url("/games/" + gameId), null, GameStateResponse.class);

        ResponseEntity<Map<String, Object>> response = exchangeForMap(
            HttpMethod.POST,
            "/games/" + gameId + "/move",
            Map.of("player", "X", "position", 99)
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
    }

    @Test
    void shouldReturn400ForMissingPlayer() {
        String gameId = "missing-player";
        restTemplate.postForEntity(url("/games/" + gameId), null, GameStateResponse.class);

        ResponseEntity<Map<String, Object>> response = exchangeForMap(
            HttpMethod.POST,
            "/games/" + gameId + "/move",
            Map.of("position", 4)
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
    }

    private ResponseEntity<Map<String, Object>> exchangeForMap(HttpMethod method, String path, Object body) {
        HttpEntity<?> request = new HttpEntity<>(body);
        return restTemplate.exchange(
            url(path),
            method,
            request,
            new ParameterizedTypeReference<>() {
            }
        );
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
