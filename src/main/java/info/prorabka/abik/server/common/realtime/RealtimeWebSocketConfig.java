package info.prorabka.abik.server.common.realtime;

import info.prorabka.abik.server.common.security.JwtService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.net.URI;
import java.util.Map;

@Configuration
@EnableWebSocket
public class RealtimeWebSocketConfig implements WebSocketConfigurer {
    private final RealtimeWebSocketHandler handler;
    private final JwtService jwtService;
    public RealtimeWebSocketConfig(RealtimeWebSocketHandler handler, JwtService jwtService) {
        this.handler = handler; this.jwtService = jwtService;
    }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws").setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        String token = queryParam(request.getURI(), "token");
                        if (token == null) return false;
                        try { attributes.put("userId", jwtService.parseUserId(token)); return true; }
                        catch (Exception e) { return false; }
                    }
                    @Override public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Exception exception) {}
                });
    }
    private static String queryParam(URI uri, String name) {
        if (uri.getQuery() == null) return null;
        for (String part : uri.getQuery().split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals(name)) return pair[1];
        }
        return null;
    }
}
