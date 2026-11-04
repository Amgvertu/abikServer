package info.prorabka.abik.server.common.realtime;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    @Override public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sessions.add(session);
        session.sendMessage(new TextMessage("{\"type\":\"ready\"}"));
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }
    @Override public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session);
    }
    /** Отправляем только сигнал изменения без данных; клиент заново читает доступные ему API. */
    public void publishChange() {
        String json = "{\"type\":\"changed\",\"at\":\"" + OffsetDateTime.now() + "\"}";
        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            try { synchronized (session) { session.sendMessage(new TextMessage(json)); } }
            catch (Exception ignored) { sessions.remove(session); }
        }
    }
}
