package com.aurora.core.adapter.security;

import com.aurora.core.contract.TenantContext;
import com.aurora.core.infrastructure.database.entity.ApiKeyEntity;
import com.aurora.core.infrastructure.security.ApiKeyService;
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
import java.util.Optional;

/**
 * API Key Authentication Filter — authenticates requests via X-API-Key header.
 *
 * <p>Runs after {@link JwtAuthenticationFilter}. If the request is already
 * authenticated (JWT), this filter does nothing. Otherwise, it attempts
 * API key authentication.
 *
 * <p>The API key is a high-entropy random string (32 bytes, base64-encoded),
 * prefixed with {@code aurora_sk_}. The SHA-256 hash is stored in the database;
 * the raw key is verified via hash lookup (not BCrypt — SHA-256 is appropriate
 * for high-entropy keys).
 *
 * <p>Security: uses SHA-256 hash lookup which is deterministic and not
 * susceptible to timing side-channels (the key itself has 256 bits of
 * entropy, and the hash comparison is done at the database level).
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyService apiKeyService;
    private final TenantContext tenantContext;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService,
                                       TenantContext tenantContext) {
        this.apiKeyService = apiKeyService;
        this.tenantContext = tenantContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Skip if already authenticated by JWT
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Optional<ApiKeyEntity> entityOpt = apiKeyService.validateApiKey(apiKey);
            if (entityOpt.isEmpty()) {
                log.debug("Invalid or expired API key on {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            ApiKeyEntity entity = entityOpt.get();

            // Build Spring Security authorities from scopes
            List<SimpleGrantedAuthority> authorities = buildAuthorities(entity.getScopes());

            // Create Spring Security Authentication
            // The principal is a synthetic user ID derived from the key ID
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "apikey:" + entity.getId(), null, authorities);
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Set tenant context — use key ID as synthetic user ID
            tenantContext.setContext(entity.getTenantId(), entity.getId());
            log.debug("API key '{}' authenticated for tenant={} on {}",
                    entity.getName(), entity.getTenantId(), request.getRequestURI());

            filterChain.doFilter(request, response);

        } finally {
            SecurityContextHolder.clearContext();
            tenantContext.clearContext();
        }
    }

    private List<SimpleGrantedAuthority> buildAuthorities(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return List.of(new SimpleGrantedAuthority("ROLE_API_KEY"));
        }
        return Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope.replace(":", "_")))
                .toList();
    }
}
