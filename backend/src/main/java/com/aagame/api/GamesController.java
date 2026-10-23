package com.aagame.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public catalogue for the original demo-only games.
 *
 * <p>This endpoint intentionally exposes no wagering, deposits, cash-outs, or
 * real-money functionality. Gameplay outcomes in this MVP are simulations.</p>
 */
@RestController
@RequestMapping("/api/games")
public class GamesController {

    private final List<GameSummary> games = List.of(
        new GameSummary("skyline-crash", "Skyline Crash", "ARCADE", "Watch the multiplier rise in a simulated round.", true),
        new GameSummary("neon-reels", "Neon Reels", "CASUAL", "A colourful slot-style demo using non-redeemable credits.", true),
        new GameSummary("reef-ranger", "Reef Ranger", "ARCADE", "A score-chasing underwater arcade demo.", true)
    );

    @GetMapping
    public GameCatalogue listGames() {
        return new GameCatalogue(games.size(), games);
    }

    public record GameSummary(
        String id,
        String title,
        String category,
        String description,
        boolean enabled
    ) {}

    public record GameCatalogue(int count, List<GameSummary> games) {}
}
