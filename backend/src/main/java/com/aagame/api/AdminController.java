package com.aagame.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JdbcTemplate jdbc;

    public AdminController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/users")
    public Map<String, Object> users(@RequestHeader(name = "Authorization", required = false) String authorization,
                                     @RequestParam(name = "query", defaultValue = "") @Size(max = 100) String query) {
        requireAdmin(authorization);
        String term = "%" + query.trim().toLowerCase(java.util.Locale.ROOT) + "%";
        List<Map<String, Object>> users = jdbc.query("""
            SELECT id,email,role,status,created_at FROM app_users
            WHERE LOWER(email) LIKE ? ORDER BY id DESC LIMIT 100
            """, (rs, row) -> Map.<String, Object>of(
                "id", rs.getLong("id"),
                "email", rs.getString("email"),
                "role", rs.getString("role"),
                "status", rs.getString("status"),
                "createdAt", rs.getTimestamp("created_at").toInstant().toString()), term);
        return Map.of("users", users, "limit", 100);
    }

    @PatchMapping("/users/{userId}/status")
    @Transactional
    public Map<String, Object> updateStatus(@RequestHeader(name = "Authorization", required = false) String authorization,
                                            @PathVariable long userId,
                                            @Valid @RequestBody StatusRequest request) {
        long actorId = requireAdmin(authorization);
        if (actorId == userId) throw new ResponseStatusException(HttpStatus.CONFLICT, "Administrators cannot change their own status.");
        var current = jdbc.query("SELECT status FROM app_users WHERE id=?", (rs, row) -> rs.getString("status"), userId);
        if (current.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        String before = current.get(0);
        if (before.equals(request.status())) return Map.of("userId", userId, "status", before, "changed", false);
        jdbc.update("UPDATE app_users SET status=? WHERE id=?", request.status(), userId);
        if ("SUSPENDED".equals(request.status())) jdbc.update("DELETE FROM user_sessions WHERE user_id=?", userId);
        jdbc.update("""
            INSERT INTO audit_events(actor_user_id,action,target_type,target_id,reason,before_value,after_value)
            VALUES (?,?,?,?,?,?,?)
            """, actorId, "USER_STATUS_CHANGED", "USER", Long.toString(userId),
            request.reason().trim(), before, request.status());
        return Map.of("userId", userId, "status", request.status(), "changed", true);
    }

    @GetMapping("/audit-events")
    public Map<String, Object> auditEvents(@RequestHeader(name = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        var events = jdbc.query("""
            SELECT id,actor_user_id,action,target_type,target_id,reason,before_value,after_value,created_at
            FROM audit_events ORDER BY id DESC LIMIT 100
            """, (rs, row) -> {
                Map<String, Object> event = new java.util.LinkedHashMap<>();
                event.put("id", rs.getLong("id"));
                event.put("actorUserId", rs.getLong("actor_user_id"));
                event.put("action", rs.getString("action"));
                event.put("targetType", rs.getString("target_type"));
                event.put("targetId", rs.getString("target_id"));
                event.put("reason", rs.getString("reason"));
                event.put("beforeValue", rs.getString("before_value"));
                event.put("afterValue", rs.getString("after_value"));
                event.put("createdAt", rs.getTimestamp("created_at").toInstant().toString());
                return event;
            });
        return Map.of("events", events, "limit", 100);
    }


    @GetMapping("/games")
    public Map<String, Object> games(@RequestHeader(name = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        var games = jdbc.query("SELECT game_id,title,category,description,enabled,updated_at FROM game_configs ORDER BY game_id",
            (rs, row) -> Map.<String, Object>of(
                "id", rs.getString("game_id"),
                "title", rs.getString("title"),
                "category", rs.getString("category"),
                "description", rs.getString("description"),
                "enabled", rs.getBoolean("enabled"),
                "updatedAt", rs.getTimestamp("updated_at").toInstant().toString()));
        return Map.of("games", games);
    }

    @PatchMapping("/games/{gameId}/config")
    @Transactional
    public Map<String, Object> updateGameConfig(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String gameId,
            @Valid @RequestBody GameConfigRequest request) {
        long actorId = requireAdmin(authorization);
        var current = jdbc.query("SELECT title,description,enabled FROM game_configs WHERE game_id=?",
            (rs, row) -> Map.<String, Object>of(
                "title", rs.getString("title"),
                "description", rs.getString("description"),
                "enabled", rs.getBoolean("enabled")), gameId);
        if (current.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game configuration not found.");
        Map<String, Object> before = current.get(0);
        String beforeValue = auditValue(before.get("title") + "|" + before.get("description") + "|" + before.get("enabled"));
        String afterValue = auditValue(request.title().trim() + "|" + request.description().trim() + "|" + request.enabled());
        jdbc.update("UPDATE game_configs SET title=?,description=?,enabled=?,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE game_id=?",
            request.title().trim(), request.description().trim(), request.enabled(), actorId, gameId);
        jdbc.update("INSERT INTO audit_events(actor_user_id,action,target_type,target_id,reason,before_value,after_value) VALUES (?,?,?,?,?,?,?)",
            actorId, "GAME_CONFIG_CHANGED", "GAME", gameId, request.reason().trim(), beforeValue, afterValue);
        return Map.of("id", gameId, "title", request.title().trim(),
            "description", request.description().trim(), "enabled", request.enabled(), "changed", true);
    }

    private static String auditValue(String value) {\n        return value.length() <= 500 ? value : value.substring(0, 500);\n    }\n\n    private long requireAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A Bearer session token is required.");
        }
        String token = authorization.substring(7).trim();
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
        var actors = jdbc.query("""
            SELECT u.id FROM user_sessions s JOIN app_users u ON u.id=s.user_id
            WHERE s.token_hash=? AND s.expires_at>CURRENT_TIMESTAMP AND u.status='ACTIVE' AND u.role='ADMIN'
            """, (rs, row) -> rs.getLong(1), hash);
        if (actors.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required.");
        return actors.get(0);
    }

    public record StatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED") String status,
        @NotBlank @Size(min = 5, max = 500) String reason
    ) {}

    public record GameConfigRequest(
        @NotBlank @Size(min = 2, max = 120) String title,
        @NotBlank @Size(max = 500) String description,
        boolean enabled,
        @NotBlank @Size(min = 5, max = 500) String reason
    ) {}
}
