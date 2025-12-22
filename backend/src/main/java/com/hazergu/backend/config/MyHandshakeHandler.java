package com.hazergu.backend.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;

public class MyHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 从握手请求的 URL 参数中获取 playerId
        // 例如请求地址是 ws://localhost:8080/ws-game?playerId=player_123
        String query = request.getURI().getQuery();
        String playerId = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("playerId");

        if (playerId == null || playerId.isEmpty()) {
            // 如果没传，给个默认随机名，防止报错
            playerId = "anonymous_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }

        final String finalPlayerId = playerId;
        return new Principal() {
            @Override
            public String getName() {
                return finalPlayerId;
            }
        };
    }
}