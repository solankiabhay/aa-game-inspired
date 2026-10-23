package com.aagame.api;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GamesController.class)
class GamesControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean JdbcTemplate jdbc;

    @BeforeEach
    void catalogueRows() {
        when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<GamesController.GameSummary>>any())).thenReturn(List.of(
            new GamesController.GameSummary("skyline-crash", "Skyline Crash", "ARCADE", "Simulated multiplier round.", true),
            new GamesController.GameSummary("neon-reels", "Neon Reels", "CASUAL", "Demo reels.", true),
            new GamesController.GameSummary("reef-ranger", "Reef Ranger", "ARCADE", "Underwater arcade demo.", true)
        ));
    }

    @Test
    void returnsEnabledDemoGamesFromConfiguredCatalogue() throws Exception {
        mockMvc.perform(get("/api/games"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(3))
            .andExpect(jsonPath("$.games[0].id").value("skyline-crash"))
            .andExpect(jsonPath("$.games[1].id").value("neon-reels"))
            .andExpect(jsonPath("$.games[2].id").value("reef-ranger"))
            .andExpect(jsonPath("$.games[0].enabled").value(true));
    }
}
