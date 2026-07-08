package com.dcm.backend.demo.webSocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class JobLogWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByJob = new ConcurrentHashMap<>();

    private String jobIdFromSession(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionsByJob.computeIfAbsent(jobIdFromSession(session), k -> new CopyOnWriteArraySet<>()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Set<WebSocketSession> sessions = sessionsByJob.get(jobIdFromSession(session));
        if (sessions != null) sessions.remove(session);
    }

    public void broadcast(String jobId, String chunk) {
        Set<WebSocketSession> sessions = sessionsByJob.get(jobId);
        if (sessions == null || sessions.isEmpty()) return;

        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) session.sendMessage(new TextMessage(chunk));
            } catch (IOException e) {
                // A broken pipe here affects only that viewer's live feed,
                // never the job itself — drop silently.
            }
        }
    }
}