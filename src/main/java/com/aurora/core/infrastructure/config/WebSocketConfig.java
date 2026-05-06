package com.aurora.core.infrastructure.config;

import com.aurora.core.adapter.websocket.WebSocketAuthInterceptor;
import com.aurora.core.adapter.websocket.YjsWebSocketHandler;
import com.aurora.core.infrastructure.collaboration.DocumentRoomManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket Configuration — registers Yjs collaboration endpoint.
 *
 * <p>Endpoint: {@code /ws/collaborate?documentId=xxx&token=jwt}
 *
 * <p>Security:
 * <ul>
 *   <li>JWT validation via {@link WebSocketAuthInterceptor}</li>
 *   <li>Tenant isolation enforced at room level</li>
 *   <li>Origin check enabled for production</li>
 * </ul>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final YjsWebSocketHandler yjsHandler;
    private final WebSocketAuthInterceptor authInterceptor;
    private final DocumentRoomManager roomManager;

    public WebSocketConfig(YjsWebSocketHandler yjsHandler,
                           WebSocketAuthInterceptor authInterceptor,
                           DocumentRoomManager roomManager) {
        this.yjsHandler = yjsHandler;
        this.authInterceptor = authInterceptor;
        this.roomManager = roomManager;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(yjsHandler, "/ws/collaborate")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("${aurora.websocket.allowed-origins:http://localhost:3000}".split(","));
    }
}
