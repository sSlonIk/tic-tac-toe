package com.example.session.domain;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SessionStore {

  private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

  public Session save(Session session) {
    sessions.put(session.getId(), session);
    return session;
  }

  public Session get(String sessionId) {
    Session session = sessions.get(sessionId);
    if (session == null) {
      throw new SessionNotFoundException(sessionId);
    }
    return session;
  }

  public int size() {
    return sessions.size();
  }

  public void clear() {
    sessions.clear();
  }
}