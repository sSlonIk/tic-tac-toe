package com.example.session.api;

import com.example.session.api.dto.SessionStateResponse;
import com.example.session.domain.Session;
import com.example.session.domain.SessionStatus;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SessionEventPublisher {

    private static final long EMITTER_TIMEOUT_MS = 60_000;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Session session) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        SessionStateResponse state = SessionStateResponse.from(session);
        if (isTerminal(session)) {
            send(emitter, state);
            emitter.complete();
            return emitter;
        }
        CopyOnWriteArrayList<SseEmitter> sessionEmitters =
            emitters.computeIfAbsent(session.getId(), id -> new CopyOnWriteArrayList<>());
        sessionEmitters.add(emitter);
        emitter.onCompletion(() -> sessionEmitters.remove(emitter));
        emitter.onTimeout(() -> sessionEmitters.remove(emitter));
        send(emitter, state);
        return emitter;
    }

    public void publish(Session session) {
        CopyOnWriteArrayList<SseEmitter> sessionEmitters = emitters.get(session.getId());
        if (sessionEmitters == null) {
            return;
        }
        SessionStateResponse state = SessionStateResponse.from(session);
        for (SseEmitter emitter : sessionEmitters) {
            send(emitter, state);
        }
        if (isTerminal(session)) {
            emitters.remove(session.getId());
            for (SseEmitter emitter : sessionEmitters) {
                emitter.complete();
            }
        }
    }

    private boolean isTerminal(Session session) {
        return session.getStatus() == SessionStatus.FINISHED || session.getStatus() == SessionStatus.FAILED;
    }

    private void send(SseEmitter emitter, SessionStateResponse state) {
        try {
            emitter.send(SseEmitter.event().name("session").data(state));
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }
}
