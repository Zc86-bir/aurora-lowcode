package com.aurora.adapter.security;

import com.aurora.core.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtTokenProvider}.
 */
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "test-jwt-secret-must-be-at-least-32-bytes-long!!";
    private static final Duration EXPIRATION = Duration.ofHours(1);

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(TEST_SECRET, EXPIRATION);
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGeneration {

        @Test
        @DisplayName("should generate valid JWT token")
        void shouldGenerateValidToken() {
            UUID userId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();

            String token = provider.generateToken(userId, tenantId, "admin",
                    "ADMIN,USER", "form:read,form:write");

            assertNotNull(token);
            assertFalse(token.isEmpty());
            // JWT has 3 parts separated by dots
            assertEquals(3, token.split("\\.").length);
        }

        @Test
        @DisplayName("should generate different tokens for different users")
        void shouldGenerateDifferentTokens() {
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();

            String token1 = provider.generateToken(user1, tenantId, "user1", "", "");
            String token2 = provider.generateToken(user2, tenantId, "user2", "", "");

            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("should validate correct token")
        void shouldValidateCorrectToken() {
            String token = provider.generateToken(UUID.randomUUID(), UUID.randomUUID(),
                    "user", "USER", "");

            assertTrue(provider.validateToken(token));
        }

        @Test
        @DisplayName("should reject token with wrong secret")
        void shouldRejectWrongSecret() {
            String token = provider.generateToken(UUID.randomUUID(), UUID.randomUUID(),
                    "user", "USER", "");

            JwtTokenProvider wrongProvider = new JwtTokenProvider(
                    "wrong-secret-must-be-at-least-32-bytes-long!!!", EXPIRATION);

            assertFalse(wrongProvider.validateToken(token));
        }

        @Test
        @DisplayName("should reject empty token")
        void shouldRejectEmptyToken() {
            // JJWT throws IllegalArgumentException for empty input
            assertThrows(Exception.class, () -> provider.validateToken(""));
        }

        @Test
        @DisplayName("should reject malformed token")
        void shouldRejectMalformedToken() {
            assertFalse(provider.validateToken("not.a.jwt"));
        }
    }

    @Nested
    @DisplayName("Claim Extraction")
    class ClaimExtraction {

        @Test
        @DisplayName("should extract userId from token")
        void shouldExtractUserId() {
            UUID userId = UUID.randomUUID();
            String token = provider.generateToken(userId, UUID.randomUUID(),
                    "user", "", "");

            assertEquals(userId, provider.extractUserId(token));
        }

        @Test
        @DisplayName("should extract tenantId from token")
        void shouldExtractTenantId() {
            UUID tenantId = UUID.randomUUID();
            String token = provider.generateToken(UUID.randomUUID(), tenantId,
                    "user", "", "");

            assertEquals(tenantId, provider.extractTenantId(token));
        }

        @Test
        @DisplayName("should extract username from token")
        void shouldExtractUsername() {
            String token = provider.generateToken(UUID.randomUUID(), UUID.randomUUID(),
                    "admin", "", "");

            assertEquals("admin", provider.extractUsername(token));
        }

        @Test
        @DisplayName("should extract roles from token")
        void shouldExtractRoles() {
            String token = provider.generateToken(UUID.randomUUID(), UUID.randomUUID(),
                    "user", "ADMIN,USER", "");

            assertEquals("ADMIN,USER", provider.extractRoles(token));
        }

        @Test
        @DisplayName("should return null for invalid token claims")
        void shouldReturnNullForInvalidToken() {
            assertNull(provider.extractUserId("invalid"));
            assertNull(provider.extractTenantId("invalid"));
            assertNull(provider.extractUsername("invalid"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should reject secret shorter than 32 bytes")
        void shouldRejectShortSecret() {
            assertThrows(IllegalArgumentException.class, () ->
                    new JwtTokenProvider("short", EXPIRATION));
        }

        @Test
        @DisplayName("should handle null roles gracefully")
        void shouldHandleNullRoles() {
            String token = provider.generateToken(UUID.randomUUID(), UUID.randomUUID(),
                    "user", null, null);

            assertNotNull(token);
            assertEquals("", provider.extractRoles(token));
        }
    }
}
