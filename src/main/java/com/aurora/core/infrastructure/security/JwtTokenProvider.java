package com.aurora.core.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token Provider — HS256 token creation and validation.
 *
 * <p>Uses JJWT 0.12.5 with HMAC-SHA256 signing.
 * Secret key is loaded from {@code aurora.security.jwt-secret} (env: JWT_SECRET).
 * Token expiration defaults to 1 hour from {@code aurora.security.jwt-expiration}.
 *
 * <p>Claims structure:
 * <ul>
 *   <li>{@code sub} — user ID (UUID)</li>
 *   <li>{@code tenant_id} — tenant ID (UUID)</li>
 *   <li>{@code username} — display name</li>
 *   <li>{@code roles} — comma-separated roles</li>
 *   <li>{@code permissions} — comma-separated permissions</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final Duration tokenExpiration;

    public JwtTokenProvider(
            @Value("${aurora.security.jwt-secret}") String jwtSecret,
            @Value("${aurora.security.jwt-expiration:1h}") Duration jwtExpiration) {
        this.signingKey = createSigningKey(jwtSecret);
        this.tokenExpiration = jwtExpiration;
        log.info("JwtTokenProvider initialized with expiration={}", jwtExpiration);
    }

    private static SecretKey createSigningKey(String jwtSecret) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 bytes for HS256, got: "
                    + keyBytes.length);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a signed JWT token.
     *
     * @param userId      the user ID (subject claim)
     * @param tenantId    the tenant ID (custom claim)
     * @param username    the display name
     * @param roles       comma-separated roles
     * @param permissions comma-separated permissions
     * @return signed JWT string
     */
    public String generateToken(UUID userId, UUID tenantId, String username,
                                String roles, String permissions) {
        Instant now = Instant.now();
        Instant expiry = now.plus(tokenExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenant_id", tenantId.toString())
                .claim("username", username)
                .claim("roles", roles != null ? roles : "")
                .claim("permissions", permissions != null ? permissions : "")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate a JWT token.
     *
     * @param token the JWT string
     * @return true if the token is valid and not expired
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token expired for subject={}", e.getClaims().getSubject());
            return false;
        } catch (JwtException e) {
            log.debug("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user ID (subject) from token.
     */
    public UUID extractUserId(String token) {
        Claims claims = parseClaims(token);
        return claims != null ? UUID.fromString(claims.getSubject()) : null;
    }

    /**
     * Extract tenant ID from token.
     */
    public UUID extractTenantId(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            return null;
        }
        String tenantId = claims.get("tenant_id", String.class);
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }

    /**
     * Extract username from token.
     */
    public String extractUsername(String token) {
        Claims claims = parseClaims(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    /**
     * Extract roles from token.
     */
    public String extractRoles(String token) {
        Claims claims = parseClaims(token);
        return claims != null ? claims.get("roles", String.class) : "";
    }

    /**
     * Extract permissions from token.
     */
    public String extractPermissions(String token) {
        Claims claims = parseClaims(token);
        return claims != null ? claims.get("permissions", String.class) : "";
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) {
            return true;
        }
        return claims.getExpiration().before(new Date());
    }

    /**
     * Get token expiration in seconds (for client-side display).
     */
    public long getTokenExpirationSeconds() {
        return tokenExpiration.getSeconds();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("Failed to parse token claims: {}", e.getMessage());
            return null;
        }
    }
}
