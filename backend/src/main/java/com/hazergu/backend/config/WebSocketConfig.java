package com.hazergu.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单消息代理，用于广播消息
        registry.enableSimpleBroker("/topic");
        // 应用目的地前缀，用于处理客户端发送的消息
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 注册WebSocket端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册WebSocket端点，允许前端连接
        registry.addEndpoint("/ws")
                // 使用具体的允许来源，与SecurityConfig一致
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174")
                .withSockJS(); // 启用SockJS，提供降级支持
    }
}
