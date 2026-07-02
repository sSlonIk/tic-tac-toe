package com.example.session.api;

import com.example.session.api.dto.CreatedSessionResponse;
import com.example.session.api.dto.SessionStateResponse;
import com.example.session.domain.Session;
import com.example.session.domain.SessionConflictException;
import com.example.session.domain.SessionStatus;
import com.example.session.domain.SessionStore;
import com.example.session.engine.EngineClient;
import com.example.session.engine.dto.GameState;
import com.example.session.simulation.SimulationService;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
public class SessionController {

  private final SessionStore sessionStore;
  private final EngineClient engineClient;
  private final SimulationService simulationService;
  private final SessionEventPublisher eventPublisher;

  public SessionController(
      SessionStore sessionStore,
      EngineClient engineClient,
      SimulationService simulationService,
      SessionEventPublisher eventPublisher
  ) {
    this.sessionStore = sessionStore;
    this.engineClient = engineClient;
    this.simulationService = simulationService;
    this.eventPublisher = eventPublisher;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreatedSessionResponse createSession() {
    String sessionId = UUID.randomUUID().toString();
    GameState createdGame = engineClient.createGame(sessionId);
    Session session = new Session(sessionId);
    session.applyCreated(createdGame);
    sessionStore.save(session);
    return new CreatedSessionResponse(sessionId, SessionStatus.CREATED);
  }

  @PostMapping("/{sessionId}/simulate")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public SessionStateResponse simulate(@PathVariable("sessionId") String sessionId) {
    Session session = sessionStore.get(sessionId);
    synchronized (session) {
      if (session.getStatus() != SessionStatus.CREATED) {
        throw new SessionConflictException("Simulation can only be started from CREATED state");
      }
      session.markRunning();
    }
    simulationService.simulate(sessionId);
    return SessionStateResponse.from(session);
  }

  @GetMapping("/{sessionId}")
  public SessionStateResponse getSession(@PathVariable("sessionId") String sessionId) {
    return SessionStateResponse.from(sessionStore.get(sessionId));
  }

  @GetMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@PathVariable("sessionId") String sessionId) {
    return eventPublisher.subscribe(sessionStore.get(sessionId));
  }
}