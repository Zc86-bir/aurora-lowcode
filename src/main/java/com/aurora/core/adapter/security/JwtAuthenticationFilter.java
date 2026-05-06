package com.aurora.core.adapter.security;

import com.aurora.core.contract.TenantContext;
import com.aurora.core.infrastructure.security.JwtTokenProvider;
import com.aurora.core.infrastructure.security.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * JWT Authentication Filter — extracts and validates JWT from Authorization header.
 *
 * <p>Validates {@code Authorization: Bearer <token>} header, extracts tenant/user
 * context, and populates both Spring SecurityContext and TenantContext.
 *
 * <p>Also checks the token blacklist to enforce logout.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TenantContext tenantContext;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                    TenantContext tenantContext,
                                    TokenBlacklistService tokenBlacklistService) {
        this.tokenProvider = tokenProvider;
        this.tenantContext = tenantContext;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!tokenProvider.validateToken(token)) {
                log.debug("Invalid JWT token on {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Check token blacklist (logout enforcement)
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.debug("Blacklisted JWT token on {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = tokenProvider.extractUserId(token);
            UUID tenantId = tokenProvider.extractTenantId(token);
            String username = tokenProvider.extractUsername(token);
            String roles = tokenProvider.extractRoles(token);

            if (userId == null || tenantId == null) {
                log.warn("Missing userId or tenantId in token for {}",
                        request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // Build Spring Security authorities from roles
            List<SimpleGrantedAuthority> authorities = buildAuthorities(roles);

            // Create Spring Security Authentication
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(), null, authorities);
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Set TenantContext for downstream use (ScopedValue propagation)
            tenantContext.setContext(tenantId, userId);
            log.debug("Authenticated user={} tenant={} on {}",
                    username, tenantId, request.getRequestURI());

            filterChain.doFilter(request, response);

        } finally {
            // Clear tenant context after request completes
            SecurityContextHolder.clearContext();
            tenantContext.clearContext();
        }
    }

    private List<SimpleGrantedAuthority> buildAuthorities(String roles) {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
