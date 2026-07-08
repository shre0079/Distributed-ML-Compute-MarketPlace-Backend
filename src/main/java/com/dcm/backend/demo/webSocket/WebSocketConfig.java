package com.dcm.backend.demo.webSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JobLogWebSocketHandler jobLogWebSocketHandler;
    private final JobLogHandshakeInterceptor jobLogHandshakeInterceptor;

    public WebSocketConfig(JobLogWebSocketHandler jobLogWebSocketHandler,
                           JobLogHandshakeInterceptor jobLogHandshakeInterceptor) {
        this.jobLogWebSocketHandler = jobLogWebSocketHandler;
        this.jobLogHandshakeInterceptor = jobLogHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(jobLogWebSocketHandler, "/ws/jobs/{jobId}")
                .addInterceptors(jobLogHandshakeInterceptor)
                .setAllowedOrigins("http://localhost:5173");
    }
}