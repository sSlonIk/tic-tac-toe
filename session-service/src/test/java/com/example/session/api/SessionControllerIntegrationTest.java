package com.example.session.api;

import com.example.session.domain.Session;
import com.example.session.domain.SessionStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@TestPropertySource(properties = {
    "engine.base-url=http://engine",
    "simulation.move-delay-ms=0"
})
class SessionControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private MockRestServiceServer mockRestServiceServer;

  @Autowired
  private SessionStore sessionStore;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    sessionStore.clear();
    mockRestServiceServer.reset();
  }

  @Test
  void createSessionShouldCallEngineAndPersistSession() throws Exception {
    mockRestServiceServer.expect(once(), requestTo(startsWith("http://engine/games/")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(
            engineState("placeholder", "IN_PROGRESS", "X", null, null, null, null, null, null, null, null, null),
            MediaType.APPLICATION_JSON
        ));

    MvcResult result = mockMvc.perform(post("/sessions"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sessionId").isNotEmpty())
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andReturn();

    String sessionId = read(result).get("sessionId").asText();

    assertThat(sessionStore.size()).isEqualTo(1);
    assertThat(sessionStore.get(sessionId).getId()).isEqualTo(sessionId);

    mockRestServiceServer.verify();
  }

  @Test
  void simulateShouldFinishAndWriteHistory() throws Exception {
    String sessionId = "simulate-success";
    sessionStore.save(new Session(sessionId));

    mockRestServiceServer.expect(once(), requestTo("http://engine/games/" + sessionId + "/move"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(
            engineState(sessionId, "IN_PROGRESS", "O", "X", null, null, null, null, null, null, null, null),
            MediaType.APPLICATION_JSON
        ));

    mockRestServiceServer.expect(once(), requestTo("http://engine/games/" + sessionId + "/move"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(
            engineState(sessionId, "IN_PROGRESS", "X", "X", "O", null, null, null, null, null, null, null),
            MediaType.APPLICATION_JSON
        ));

    mockRestServiceServer.expect(once(), requestTo("http://engine/games/" + sessionId + "/move"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(
            engineState(sessionId, "IN_PROGRESS", "O", "X", "O", "X", null, null, null, null, null, null),
            MediaType.APPLICATION_JSON
        ));

    mockRestServiceServer.expect(once(), requestTo("http://engine/games/" + sessionId + "/move"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(
            engineState(sessionId, "IN_PROGRESS", "X", "X", "O", "X", "O", null, null, null, null, null),
            MediaType.APPLICATION_JSON
        ));

    mockRestServiceServer.expect(once(), requestTo("http://engine/games/" + sessionId + "/move"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(
            engineState(sessionId, "X_WON", null, "X", "O", "X", "O", "X", null, null, null, null),
            MediaType.APPLICATION_JSON
        ));

    mockMvc.perform(post("/sessions/{sessionId}/simulate", sessionId))
        .andExpect(status().isAccepted());

    JsonNode session = awaitStatus(sessionId, "FINISHED");

    assertThat(session.get("gameStatus").asText()).isEqualTo("X_WON");
    assertThat(session.get("history").size()).isEqualTo(5);
    assertTrue(session.get("failureReason").isNull());

    mockRestServiceServer.verify();
  }

  @Test
  void shouldFailSessionWhenEngineFailsMidGame() throws Exception {
    String sessionId = "simulate-failure";
    sessionStore.save(new Session(sessionId));

    mockRestServiceServer.expect(once(), requestTo("http://engine/games/" + sessionId + "/move"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(
            engineState(sessionId, "IN_PROGRESS", "O", "X", null, null, null, null, null, null, null, null),
            MediaType.APPLICATION_JSON
        ));

    mockRestServiceServer.expect(once(), requestTo("http://engine/games/" + sessionId + "/move"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body("""
                    {
                      "type":"about:blank",
                      "title":"Internal Server Error",
                      "status":500,
                      "detail":"Engine failed"
                    }
                    """));

    mockMvc.perform(post("/sessions/{sessionId}/simulate", sessionId))
        .andExpect(status().isAccepted());

    JsonNode session = awaitStatus(sessionId, "FAILED");

    assertThat(session.get("failureReason").asText()).isNotBlank();

    mockRestServiceServer.verify();
  }

  @Test
  void shouldReturn502AndNotPersistSessionWhenEngineCreateFails() throws Exception {
    mockRestServiceServer.expect(once(), requestTo(startsWith("http://engine/games/")))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body("""
                    {
                      "type":"about:blank",
                      "title":"Internal Server Error",
                      "status":500,
                      "detail":"Engine create failed"
                    }
                    """));

    mockMvc.perform(post("/sessions"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status").value(502));

    assertThat(sessionStore.size()).isZero();

    mockRestServiceServer.verify();
  }

  @Test
  void shouldRejectRepeatedSimulation() throws Exception {
    String sessionId = "already-running";
    Session session = new Session(sessionId);
    session.markRunning();
    sessionStore.save(session);

    mockMvc.perform(post("/sessions/{sessionId}/simulate", sessionId))
        .andExpect(status().isConflict());
  }

  private JsonNode awaitStatus(String sessionId, String expectedStatus) throws Exception {
    for (int i = 0; i < 100; i++) {
      MvcResult result = mockMvc.perform(get("/sessions/{sessionId}", sessionId))
          .andExpect(status().isOk())
          .andReturn();

      JsonNode json = read(result);
      if (expectedStatus.equals(json.path("status").asText())) {
        return json;
      }

      Thread.sleep(25);
    }

    throw new AssertionError("Session did not reach status " + expectedStatus);
  }

  private JsonNode read(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsByteArray());
  }

  private String engineState(
      String gameId,
      String status,
      String nextPlayer,
      Object c0,
      Object c1,
      Object c2,
      Object c3,
      Object c4,
      Object c5,
      Object c6,
      Object c7,
      Object c8
  ) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("gameId", gameId);
    body.put("board", java.util.Arrays.asList(c0, c1, c2, c3, c4, c5, c6, c7, c8));
    body.put("status", status);
    body.put("nextPlayer", nextPlayer);
    return objectMapper.writeValueAsString(body);
  }
}