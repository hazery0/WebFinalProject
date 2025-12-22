package com.hazergu.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的内存消息代理，处理以 /topic 开头的消息
        config.enableSimpleBroker("/topic", "/queue");
        // 设置应用程序目的地前缀，客户端发送消息需要以 /app 开头
        config.setApplicationDestinationPrefixes("/app");
        // 设置用户目的地前缀（用于私聊）
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 STOMP 端点，客户端将通过这个端点连接
        registry.addEndpoint("/ws-game")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new MyHandshakeHandler())
                .withSockJS() // 启用 SockJS 回退选项
                .setSuppressCors(false);
    }
}
