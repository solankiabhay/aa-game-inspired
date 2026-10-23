package com.aagame.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GamesController.class)
class GamesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsOnlyEnabledDemoGames() throws Exception {
        mockMvc.perform(get("/api/games"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(3))
            .andExpect(jsonPath("$.games[0].id").value("skyline-crash"))
            .andExpect(jsonPath("$.games[1].id").value("neon-reels"))
            .andExpect(jsonPath("$.games[2].id").value("reef-ranger"))
            .andExpect(jsonPath("$.games[0].enabled").value(true));
    }
}
