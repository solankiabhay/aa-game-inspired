package com.aagame.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final JavaMailSender mailSender;
    private final boolean emailVerificationEnabled;
    private final boolean passwordResetEnabled;
    private final String publicBaseUrl;
    private final String mailFrom;

    public AuthController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, JavaMailSender mailSender,
            @Value("${app.security.email-verification-enabled:false}") boolean emailVerificationEnabled,
            @Value("${app.security.password-reset-enabled:false}") boolean passwordResetEnabled,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${app.mail.from:no-reply@example.invalid}") String mailFrom) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.emailVerificationEnabled = emailVerificationEnabled;
        this.passwordResetEnabled = passwordResetEnabled;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
        this.mailFrom = mailFrom;
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
                    "INSERT INTO app_users(email,password_hash,role,status,email_verified) VALUES (?, ?, 'USER', 'ACTIVE', ?)",
                    new String[] {"ID"});
                ps.setString(1, email);
                ps.setString(2, passwordHash);
                ps.setBoolean(3, !emailVerificationEnabled);
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        long userId = keyHolder.getKey().longValue();
        jdbc.update("INSERT INTO demo_credit_accounts(user_id,balance) VALUES (?,?)", userId, STARTING_DEMO_CREDITS);
        jdbc.update("INSERT INTO demo_credit_ledger(user_id,delta,balance_after,reason,reference_id) VALUES (?,?,?,'WELCOME_CREDITS',?)",
            userId, STARTING_DEMO_CREDITS, STARTING_DEMO_CREDITS, "registration:" + userId);
        if (emailVerificationEnabled) {
            issueActionToken(userId, email, "EMAIL_VERIFY", Duration.ofHours(24),
                "Verify your AA Game account", "Use this one-time link to verify your email: " + publicBaseUrl + "/verify-email.html#token=");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "status", emailVerificationEnabled ? "VERIFICATION_REQUIRED" : "REGISTERED",
            "email", email, "startingDemoCredits", STARTING_DEMO_CREDITS));
    }

    @PostMapping("/login")
    public SessionResponse login(@Valid @RequestBody LoginRequest request) {
        String email = normalizeEmail(request.email());
        var users = jdbc.query("SELECT id,password_hash,status,email_verified FROM app_users WHERE email = ?",
            (rs, row) -> new UserRow(rs.getLong("id"), null, null, rs.getString("status"), rs.getString("password_hash"), rs.getBoolean("email_verified")), email);
        String hashToCheck = users.isEmpty() ? dummyPasswordHash : users.get(0).passwordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);
        if (users.isEmpty() || !"ACTIVE".equals(users.get(0).status()) || !passwordMatches || (emailVerificationEnabled && !users.get(0).emailVerified())) {
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

    @Transactional
    @PostMapping("/verify-email")
    public Map<String, String> verifyEmail(@Valid @RequestBody TokenRequest request) {
        ActionToken token = consumeActionToken(request.token(), "EMAIL_VERIFY");
        jdbc.update("UPDATE app_users SET email_verified=TRUE WHERE id=?", token.userId());
        return Map.of("status", "EMAIL_VERIFIED", "message", "Email verified. You can now sign in.");
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        String email = normalizeEmail(request.email());
        if (passwordResetEnabled) {
            var users = jdbc.query("SELECT id FROM app_users WHERE email=?", (rs, row) -> rs.getLong("id"), email);
            if (!users.isEmpty()) {
                try {
                    issueActionToken(users.get(0), email, "PASSWORD_RESET", Duration.ofMinutes(30),
                        "Reset your AA Game password", "Use this one-time link to reset your password: " + publicBaseUrl + "/reset-password.html#token=");
                } catch (MailException ex) {
                    // Keep the response identical for known and unknown accounts; remove unusable token records.
                    jdbc.update("DELETE FROM auth_action_tokens WHERE user_id=? AND purpose='PASSWORD_RESET' AND used_at IS NULL", users.get(0));
                }
            }
        }
        return ResponseEntity.accepted().body(Map.of("status", "REQUEST_ACCEPTED",
            "message", "If an eligible account exists, password-reset instructions will be sent."));
    }

    @Transactional
    @PostMapping("/password-reset/confirm")
    public Map<String, String> confirmPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        ActionToken token = consumeActionToken(request.token(), "PASSWORD_RESET");
        jdbc.update("UPDATE app_users SET password_hash=? WHERE id=?",
            passwordEncoder.encode(request.newPassword()), token.userId());
        jdbc.update("DELETE FROM user_sessions WHERE user_id=?", token.userId());
        return Map.of("status", "PASSWORD_RESET", "message", "Password changed. Sign in with your new password.");
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
            SELECT u.id,u.email,u.role,u.status,u.email_verified
            FROM user_sessions s JOIN app_users u ON u.id=s.user_id
            WHERE s.token_hash=? AND s.expires_at > CURRENT_TIMESTAMP
            """, (rs, row) -> new UserRow(rs.getLong("id"), rs.getString("email"),
                rs.getString("role"), rs.getString("status"), null, rs.getBoolean("email_verified")), sha256(token));
        if (users.isEmpty() || !"ACTIVE".equals(users.get(0).status())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is invalid or expired.");
        }
        return users.get(0);
    }

    private void issueActionToken(long userId, String email, String purpose, Duration lifetime, String subject, String bodyPrefix) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("INSERT INTO auth_action_tokens(token_hash,user_id,purpose,expires_at) VALUES (?,?,?,?)",
            sha256(token), userId, purpose, Timestamp.from(Instant.now().plus(lifetime)));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(bodyPrefix + token + "\n\nIf you did not request this, you can ignore this message.");
        mailSender.send(message);
    }

    private ActionToken consumeActionToken(String rawToken, String purpose) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This link is invalid or expired. Request a new one.");
        }
        var tokens = jdbc.query("SELECT user_id FROM auth_action_tokens WHERE token_hash=? AND purpose=? AND used_at IS NULL AND expires_at>CURRENT_TIMESTAMP",
            (rs, row) -> new ActionToken(rs.getLong("user_id")), sha256(rawToken), purpose);
        if (tokens.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This link is invalid or expired. Request a new one.");
        int consumed = jdbc.update("UPDATE auth_action_tokens SET used_at=CURRENT_TIMESTAMP WHERE token_hash=? AND purpose=? AND used_at IS NULL AND expires_at>CURRENT_TIMESTAMP", sha256(rawToken), purpose);
        if (consumed != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This link is invalid or expired. Request a new one.");
        return tokens.get(0);
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
    public record TokenRequest(@NotBlank @Size(max = 128) String token) {}
    public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record PasswordResetRequest(@NotBlank @Size(max = 128) String token,
                                       @NotBlank @Size(min = 12, max = 72) String newPassword) {}
    private record ActionToken(long userId) {}
    private record UserRow(long id, String email, String role, String status, String passwordHash, boolean emailVerified) {}
}

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex, HttpServletRequest request) {
        String message = ex.getReason() == null ? "Request could not be completed." : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
            "code", ex.getStatusCode().toString(), "message", message, "path", request.getRequestURI(),
            "requestId", UUID.randomUUID().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> Map.of("field", error.getField(), "code", "INVALID_VALUE"))
            .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(Map.of(
            "code", "VALIDATION_ERROR",
            "message", "Please check the submitted fields.",
            "requestId", UUID.randomUUID().toString(),
            "path", request.getRequestURI(),
            "fieldErrors", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(Map.of(
            "code", "MALFORMED_REQUEST",
            "message", "The request body is missing or invalid.",
            "requestId", UUID.randomUUID().toString(),
            "path", request.getRequestURI()));
    }
}
