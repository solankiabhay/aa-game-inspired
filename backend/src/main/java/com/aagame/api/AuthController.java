package com.aagame.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SESSION_HOURS = 24;
    private static final long STARTING_DEMO_CREDITS = 1000;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public AuthController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-user-password-for-timing");
    }

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String passwordHash = passwordEncoder.encode(request.password());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO app_users(email,password_hash,role,status) VALUES (?,?, 'USER','ACTIVE')",
                    new String[] {"ID"});
                ps.setString(1, email);
                ps.setString(2, passwordHash);
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        long userId = keyHolder.getKey().longValue();
        jdbc.update("INSERT INTO demo_credit_accounts(user_id,balance) VALUES (?,?)", userId, STARTING_DEMO_CREDITS);
        jdbc.update("INSERT INTO demo_credit_ledger(user_id,delta,balance_after,reason,reference_id) VALUES (?,?,?,'WELCOME_CREDITS',?)",
            userId, STARTING_DEMO_CREDITS, STARTING_DEMO_CREDITS, "registration:" + userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "status", "REGISTERED", "email", email, "startingDemoCredits", STARTING_DEMO_CREDITS));
    }

    @PostMapping("/login")
    public SessionResponse login(@Valid @RequestBody LoginRequest request) {
        String email = normalizeEmail(request.email());
        var users = jdbc.query("SELECT id,password_hash,status FROM app_users WHERE email = ?",
            (rs, row) -> new UserRow(rs.getLong("id"), null, null, rs.getString("status"), rs.getString("password_hash")), email);
        String hashToCheck = users.isEmpty() ? dummyPasswordHash : users.get(0).passwordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);
        if (users.isEmpty() || !"ACTIVE".equals(users.get(0).status()) || !passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        UserRow user = users.get(0);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = Instant.now().plus(SESSION_HOURS, ChronoUnit.HOURS);
        jdbc.update("INSERT INTO user_sessions(token_hash,user_id,expires_at) VALUES (?,?,?)",
            sha256(token), user.id(), Timestamp.from(expires));
        return new SessionResponse(token, "Bearer", expires.toString());
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(name = "Authorization", required = false) String authorization) {
        UserRow user = requireUser(authorization);
        return Map.of("id", user.id(), "email", user.email(), "role", user.role());
    }

    @GetMapping("/me/demo-credits")
    public Map<String, Object> demoCredits(@RequestHeader(name = "Authorization", required = false) String authorization) {
        UserRow user = requireUser(authorization);
        var balances = jdbc.query("SELECT balance FROM demo_credit_accounts WHERE user_id=?",
            (rs, row) -> rs.getLong("balance"), user.id());
        if (balances.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demo-credit account not found.");
        return Map.of("balance", balances.get(0), "currency", "DEMO_CREDITS", "redeemable", false);
    }

    @GetMapping("/me/ledger")
    public Map<String, Object> demoLedger(@RequestHeader(name = "Authorization", required = false) String authorization) {
        UserRow user = requireUser(authorization);
        var entries = jdbc.query("SELECT delta,balance_after,reason,reference_id,created_at FROM demo_credit_ledger WHERE user_id=? ORDER BY id DESC LIMIT 100",
            (rs, row) -> Map.<String, Object>of(
                "delta", rs.getLong("delta"),
                "balanceAfter", rs.getLong("balance_after"),
                "reason", rs.getString("reason"),
                "referenceId", rs.getString("reference_id") == null ? "" : rs.getString("reference_id"),
                "createdAt", rs.getTimestamp("created_at").toInstant().toString()), user.id());
        return Map.of("entries", entries, "limit", 100);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = "Authorization", required = false) String authorization) {
        jdbc.update("DELETE FROM user_sessions WHERE token_hash = ?", sha256(bearerToken(authorization)));
        return ResponseEntity.noContent().build();
    }

    private UserRow requireUser(String authorization) {
        String token = bearerToken(authorization);
        var users = jdbc.query("""
            SELECT u.id,u.email,u.role,u.status
            FROM user_sessions s JOIN app_users u ON u.id=s.user_id
            WHERE s.token_hash=? AND s.expires_at > CURRENT_TIMESTAMP
            """, (rs, row) -> new UserRow(rs.getLong("id"), rs.getString("email"),
                rs.getString("role"), rs.getString("status"), null), sha256(token));
        if (users.isEmpty() || !"ACTIVE".equals(users.get(0).status())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is invalid or expired.");
        }
        return users.get(0);
    }

    private static String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A Bearer session token is required.");
        }
        return authorization.substring(7).trim();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String sha256(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public record RegisterRequest(@NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Size(min = 12, max = 72) String password) {}
    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 72) String password) {}
    public record SessionResponse(String accessToken, String tokenType, String expiresAt) {}
    private record UserRow(long id, String email, String role, String status, String passwordHash) {}
}

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex, HttpServletRequest request) {
        String message = ex.getReason() == null ? "Request could not be completed." : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
            "code", ex.getStatusCode().toString(), "message", message, "path", request.getRequestURI()));
    }
}
