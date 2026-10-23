package com.aagame.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/games")
public class GameSessionsController {
    private static final List<String> ALLOWED_GAMES = List.of("skyline-crash", "neon-reels", "reef-ranger");
    private final JdbcTemplate jdbc;

    public GameSessionsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/{gameId}/sessions")
    public ResponseEntity<?> start(@PathVariable String gameId,
                                   @RequestHeader(name = "Authorization", required = false) String authorization) {
        long userId = userId(authorization);
        if (!ALLOWED_GAMES.contains(gameId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found.");
        }
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO game_sessions(id,user_id,game_id,status,score) VALUES (?,?,?,'ACTIVE',0)", id, userId, gameId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "sessionId", id, "gameId", gameId, "status", "ACTIVE", "score", 0));
    }

    @GetMapping("/sessions/{sessionId}")
    public Map<String, Object> getSession(@PathVariable String sessionId,
                                          @RequestHeader(name = "Authorization", required = false) String authorization) {
        long userId = userId(authorization);
        var rows = jdbc.query("SELECT id,game_id,status,score,created_at,finished_at FROM game_sessions WHERE id=? AND user_id=?",
            (rs, row) -> {
                Map<String, Object> value = new java.util.LinkedHashMap<>();
                value.put("sessionId", rs.getString("id"));
                value.put("gameId", rs.getString("game_id"));
                value.put("status", rs.getString("status"));
                value.put("score", rs.getLong("score"));
                value.put("createdAt", rs.getTimestamp("created_at").toInstant().toString());
                var finished = rs.getTimestamp("finished_at");
                value.put("finishedAt", finished == null ? null : finished.toInstant().toString());
                return value;
            }, sessionId, userId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game session not found.");
        }
        return rows.get(0);
    }

    private long userId(String authorization) {
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
        var users = jdbc.query("""
            SELECT u.id FROM user_sessions s JOIN app_users u ON u.id=s.user_id
            WHERE s.token_hash=? AND s.expires_at > CURRENT_TIMESTAMP AND u.status='ACTIVE'
            """, (rs, row) -> rs.getLong(1), hash);
        if (users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session is invalid or expired.");
        }
        return users.get(0);
    }
}
