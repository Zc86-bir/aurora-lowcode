package com.aurora.core.adapter.websocket;

import com.aurora.core.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket Auth Interceptor — validates JWT during handshake.
 *
 * <p>Extracts token from query parameter {@code ?token=xxx} or
 * {@code Authorization} header, validates it, and stores
 * {@code tenantId}, {@code userId}, and {@code documentId}
 * in the WebSocket session attributes.
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtTokenProvider tokenProvider;

    public WebSocketAuthInterceptor(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || !tokenProvider.validateToken(token)) {
            log.debug("WebSocket handshake rejected: invalid or missing token");
            return false;
        }

        UUID userId = tokenProvider.extractUserId(token);
        UUID tenantId = tokenProvider.extractTenantId(token);

        if (userId == null || tenantId == null) {
            log.debug("WebSocket handshake rejected: missing userId or tenantId in token");
            return false;
        }

        // Extract documentId from query parameter
        String documentId = extractQueryParam(request, "documentId");
        if (documentId == null || documentId.isBlank()) {
            log.debug("WebSocket handshake rejected: missing documentId");
            return false;
        }

        // Store in session attributes for the handler
        attributes.put("userId", userId.toString());
        attributes.put("tenantId", tenantId.toString());
        attributes.put("documentId", documentId);

        log.debug("WebSocket handshake accepted: user={} tenant={} doc={}",
                userId, tenantId, documentId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }

    private String extractToken(ServerHttpRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Try query parameter
        return extractQueryParam(request, "token");
    }

    private String extractQueryParam(ServerHttpRequest request, String param) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getParameter(param);
        }
        // Fallback for non-servlet (e.g., WebFlux)
        String query = request.getURI().getQuery();
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) {
                return kv[1];
            }
        }
        return null;
    }
}
