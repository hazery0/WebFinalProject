package com.hazergu.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/ws-game/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "http://localhost",
                        "http://127.0.0.1"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600);

        // 处理 SockJS 的特殊路径
        registry.addMapping("/ws-game/info")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600);
    }
}