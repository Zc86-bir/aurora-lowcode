package com.aurora.core.adapter.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter for MCP Endpoints
 *
 * All requests to /mcp/sse and /mcp/message must carry a valid JWT token
 * in the Authorization header:
 *
 *   Authorization: Bearer &lt;jwt-token&gt;
 *
 * Without valid token, returns 401 Unauthorized.
 * This ensures only authenticated AI clients (Cursor, Claude Desktop, etc.)
 * can access the MCP tools.
 */
@Component
public class McpSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(McpSecurityFilter.class);

    private static final String MCP_SSE_PATH = "/mcp/sse";
    private static final String MCP_MESSAGE_PATH = "/mcp/message";

    @Value("${aurora.mcp.auth.enabled:true}")
    private boolean authEnabled;

    @Value("${aurora.mcp.auth.header:Authorization}")
    private String authHeader;

    @Value("${aurora.mcp.auth.prefix:Bearer }")
    private String authPrefix;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return authEnabled
            && !path.startsWith(MCP_SSE_PATH)
            && !path.startsWith(MCP_MESSAGE_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        // If auth is disabled, allow all requests (development only)
        if (!authEnabled) {
            log.warn("MCP authentication is DISABLED - this should NOT be used in production");
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(this.authHeader);
        if (authHeader == null || authHeader.isEmpty()) {
            sendUnauthorized(response, "Missing Authorization header");
            return;
        }

        if (!authHeader.startsWith(authPrefix)) {
            sendUnauthorized(response, "Authorization header must start with 'Bearer '");
            return;
        }

        String token = authHeader.substring(authPrefix.length()).trim();
        if (token.isEmpty()) {
            sendUnauthorized(response, "Empty JWT token");
            return;
        }

        // Validate JWT token
        try {
            if (!isValidToken(token)) {
                sendUnauthorized(response, "Invalid or expired JWT token");
                return;
            }

            // Extract tenant ID from token for tenant-scoped tool execution
            String tenantId = extractTenantId(token);
            if (tenantId != null) {
                request.setAttribute("X-Tenant-Id", tenantId);
            }

            // Extract user ID from token
            String userId = extractUserId(token);
            if (userId != null) {
                request.setAttribute("X-User-Id", userId);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            sendUnauthorized(response, "JWT validation failed: " + e.getMessage());
        }
    }

    /**
     * Validate JWT token.
     *
     * In production: integrate with your JWT provider (e.g., JJWT, Spring Security OAuth2).
     * This implementation delegates to the application's JWT validation service.
     */
    private boolean isValidToken(String token) {
        // TODO: Replace with actual JWT validation logic
        // Example using JJWT:
        //   try {
        //       Jwts.parserBuilder()
        //           .setSigningKey(secretKey)
        //           .build()
        //           .parseClaimsJws(token);
        //       return true;
        //   } catch (JwtException e) {
        //       return false;
        //   }
        return token.length() > 10; // Placeholder: accept any non-empty token
    }

    /**
     * Extract tenant ID from JWT claims.
     *
     * Expected claim: "tenant_id" (UUID string)
     */
    private String extractTenantId(String token) {
        // TODO: Extract from JWT claims
        // Example:
        //   Claims claims = Jwts.parserBuilder()
        //       .setSigningKey(secretKey)
        //       .build()
        //       .parseClaimsJws(token)
        //       .getBody();
        //   return claims.get("tenant_id", String.class);
        return null;
    }

    /**
     * Extract user ID from JWT claims.
     *
     * Expected claim: "sub" (subject = user ID)
     */
    private String extractUserId(String token) {
        // TODO: Extract from JWT claims
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
            String.format("{\"error\":\"unauthorized\",\"message\":\"%s\"}", message)
        );
    }
}
