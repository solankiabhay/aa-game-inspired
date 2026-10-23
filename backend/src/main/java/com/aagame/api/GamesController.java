package com.aagame.api;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public catalogue for original demo-only games, read from database configuration. */
@RestController
@RequestMapping("/api/games")
public class GamesController {
    private final JdbcTemplate jdbc;

    public GamesController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public GameCatalogue listGames() {
        List<GameSummary> games = jdbc.query("""
            SELECT game_id,title,category,description,enabled
            FROM game_configs WHERE enabled=TRUE ORDER BY game_id
            """, (rs, row) -> new GameSummary(
                rs.getString("game_id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getBoolean("enabled")));
        return new GameCatalogue(games.size(), games);
    }

    public record GameSummary(String id, String title, String category, String description, boolean enabled) {}
    public record GameCatalogue(int count, List<GameSummary> games) {}
}
