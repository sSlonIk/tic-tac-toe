package com.example.session.api;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.engine.EngineApplication;
import com.example.session.api.dto.CreatedSessionResponse;
import com.example.session.api.dto.SessionStateResponse;
import com.example.session.domain.GameStatus;
import com.example.session.domain.Move;
import com.example.session.domain.Player;
import com.example.session.domain.SessionStatus;
import java.time.Duration;
import java.util.EnumSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FullGameE2ETest {

    private static ConfigurableApplicationContext engineContext;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("engine.base-url", FullGameE2ETest::engineBaseUrl);
        registry.add("simulation.move-delay-ms", () -> 0);
    }

    @AfterAll
    static void stopEngine() {
        if (engineContext != null) {
            engineContext.close();
        }
    }

    @Test
    void fullGameShouldFinishWithConsistentBoardAndHistory() {
        CreatedSessionResponse created = restTemplate.postForObject(url("/sessions"), null, CreatedSessionResponse.class);
        assertNotNull(created);

        restTemplate.postForEntity(url("/sessions/" + created.sessionId() + "/simulate"), null, SessionStateResponse.class);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            SessionStateResponse state = restTemplate.getForObject(url("/sessions/" + created.sessionId()), SessionStateResponse.class);
            assertNotNull(state);
            assertEquals(SessionStatus.FINISHED, state.status());
            assertTrue(EnumSet.of(GameStatus.X_WON, GameStatus.O_WON, GameStatus.DRAW).contains(state.gameStatus()));
            assertTrue(state.history().size() >= 5 && state.history().size() <= 9);
            assertBoardMatchesHistory(state);
        });
    }

    private static String engineBaseUrl() {
        if (engineContext == null) {
            engineContext = new SpringApplicationBuilder(EngineApplication.class)
                .properties("server.port=0")
                .run();
        }
        return "http://localhost:" + engineContext.getEnvironment().getProperty("local.server.port");
    }

    private void assertBoardMatchesHistory(SessionStateResponse state) {
        Player[] expectedBoard = new Player[9];
        for (Move move : state.history()) {
            expectedBoard[move.position()] = move.player();
        }
        assertEquals(expectedBoard.length, state.board().length);
        for (int i = 0; i < expectedBoard.length; i++) {
            assertEquals(expectedBoard[i], state.board()[i]);
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
