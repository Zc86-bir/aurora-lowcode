package com.aurora.core.adapter.mcp;

import com.aurora.core.contract.TenantContext;
import com.aurora.core.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * JWT Authentication Filter for MCP Endpoints.
 *
 * <p>All requests to {@code /mcp/sse} and {@code /mcp/message} must carry a valid
 * JWT token in the {@code Authorization: Bearer <token>} header.
 *
 * <p>Validates token via {@link JwtTokenProvider}, extracts tenant/user context,
 * and populates both SecurityContext and TenantContext.
 */
@Component
public class McpSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(McpSecurityFilter.class);

    private static final String MCP_SSE_PATH = "/mcp/sse";
    private static final String MCP_MESSAGE_PATH = "/mcp/message";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TenantContext tenantContext;

    @Value("${aurora.mcp.auth.enabled:true}")
    private boolean authEnabled;

    public McpSecurityFilter(JwtTokenProvider tokenProvider,
                             TenantContext tenantContext) {
        this.tokenProvider = tokenProvider;
        this.tenantContext = tenantContext;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !authEnabled
            || (!path.startsWith(MCP_SSE_PATH)
                && !path.startsWith(MCP_MESSAGE_PATH));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        if (!authEnabled) {
            log.warn("MCP authentication is DISABLED — do NOT use in production");
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            sendUnauthorized(response, "Empty JWT token");
            return;
        }

        try {
            if (!tokenProvider.validateToken(token)) {
                sendUnauthorized(response, "Invalid or expired JWT token");
                return;
            }

            UUID userId = tokenProvider.extractUserId(token);
            UUID tenantId = tokenProvider.extractTenantId(token);
            String roles = tokenProvider.extractRoles(token);

            if (userId == null || tenantId == null) {
                sendUnauthorized(response, "Missing userId or tenantId in token");
                return;
            }

            // Populate Spring SecurityContext
            List<SimpleGrantedAuthority> authorities = Arrays.stream(
                    roles.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Populate TenantContext for downstream use
            tenantContext.setContext(tenantId, userId);

            // Set request attributes for MCP tool execution
            request.setAttribute("X-Tenant-Id", tenantId.toString());
            request.setAttribute("X-User-Id", userId.toString());

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("MCP JWT validation failed: {}", e.getMessage());
            sendUnauthorized(response, "JWT validation failed");
        } finally {
            SecurityContextHolder.clearContext();
            tenantContext.clearContext();
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
            String.format("{\"error\":\"unauthorized\",\"message\":\"%s\"}",
                    message));
    }
}
