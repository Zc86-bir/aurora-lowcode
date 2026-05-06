package com.aurora.core.adapter.web;

import com.aurora.core.contract.TenantContext;
import com.aurora.core.infrastructure.security.JwtTokenProvider;
import com.aurora.core.infrastructure.security.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication Controller — login, logout, token refresh.
 *
 * <p>{@code POST /auth/login} — accepts username+password, validates via BCrypt,
 * returns JWT token + tenant info.
 *
 * <p>{@code POST /auth/logout} — adds token to in-memory blacklist (for stateless
 * JWT, this is best-effort; production should use Redis-backed blacklist).
 *
 * <p>Seed accounts: admin@aurora.dev / admin123 (force_password_change=true).
 */
@Tag(name = "Authentication", description = "JWT login and logout endpoints")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TenantContext tenantContext;
    private final TokenBlacklistService tokenBlacklistService;

    // In-memory user store — replace with JPA repository in production
    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();

    public AuthController(JwtTokenProvider tokenProvider,
                           PasswordEncoder passwordEncoder,
                           TenantContext tenantContext,
                           TokenBlacklistService tokenBlacklistService) {
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.tenantContext = tenantContext;
        this.tokenBlacklistService = tokenBlacklistService;
        initializeSeedUsers();
    }

    @Operation(summary = "Login with username and password",
               description = "Returns JWT token and tenant info on success")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (request.username == null || request.username.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username is required"));
        }
        if (request.password == null || request.password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password is required"));
        }

        UserRecord user = users.get(request.username.toLowerCase());
        if (user == null) {
            log.debug("Login attempt for non-existent user: {}", request.username);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid credentials"));
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash())) {
            log.debug("Failed login attempt for user: {}", request.username);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid credentials"));
        }

        if (user.forcePasswordChange()) {
            log.info("User {} must change password", request.username);
            return ResponseEntity.ok(Map.of(
                    "requiresPasswordChange", true,
                    "message", "Please change your password"
            ));
        }

        // Build roles and permissions strings
        String roles = String.join(",", user.roles());
        String permissions = String.join(",", user.permissions());

        // Generate JWT
        String token = tokenProvider.generateToken(
                user.userId(),
                user.tenantId(),
                user.username(),
                roles,
                permissions
        );

        log.info("User {} logged in successfully, tenant={}",
                request.username, user.tenantId());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "tokenType", "Bearer",
                "expiresIn", tokenProvider.getTokenExpirationSeconds(),
                "tenantId", user.tenantId().toString(),
                "tenantCode", user.tenantCode(),
                "userId", user.userId().toString(),
                "username", user.username(),
                "roles", user.roles()
        ));
    }

    @Operation(summary = "Logout (invalidate token)",
               description = "Adds current token to blacklist")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization",
                           required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.blacklist(token);
            log.info("Token added to blacklist");
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @Operation(summary = "Check if token is valid",
               description = "Returns current user info from token")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        UUID userId = tenantContext.getCurrentUserId();
        UUID tenantId = tenantContext.getCurrentTenantId();

        if (userId == null || tenantId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not authenticated"));
        }

        return ResponseEntity.ok(Map.of(
                "userId", userId.toString(),
                "tenantId", tenantId.toString(),
                "authenticated", true
        ));
    }

    private void initializeSeedUsers() {
        String adminHash = passwordEncoder.encode("admin123");
        users.put("admin@aurora.dev", new UserRecord(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "admin@aurora.dev",
                adminHash,
                Set.of("ADMIN", "USER"),
                Set.of("form:read", "form:write", "report:read",
                       "report:write", "workflow:read", "workflow:write",
                       "metadata:read", "metadata:write", "admin:users"),
                "Default",
                false
        ));
        log.info("Seed user initialized: admin@aurora.dev");
    }

    // Request DTO — deserialized from JSON body by Jackson
    public static class LoginRequest {
        public String username;
        public String password;
    }

    private record UserRecord(
            UUID userId,
            UUID tenantId,
            String username,
            String passwordHash,
            Set<String> roles,
            Set<String> permissions,
            String tenantCode,
            boolean forcePasswordChange
    ) {}
}
