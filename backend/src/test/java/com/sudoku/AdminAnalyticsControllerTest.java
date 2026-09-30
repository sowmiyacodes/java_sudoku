package com.sudoku;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetOverviewStats() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPlayers").exists())
                .andExpect(jsonPath("$.totalGames").exists())
                .andExpect(jsonPath("$.mlModels").exists());
    }

    @Test
    public void testGetAnalyticsCharts() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/charts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusDistribution").exists());
    }

    @Test
    public void testGetPlayers() throws Exception {
        mockMvc.perform(get("/api/admin/players"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetGames() throws Exception {
        mockMvc.perform(get("/api/admin/games"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetHints() throws Exception {
        mockMvc.perform(get("/api/admin/hints"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetPuzzles() throws Exception {
        mockMvc.perform(get("/api/admin/puzzles"))
                .andExpect(status().isOk());
    }

    @Test
    public void testAuditLogsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isOk());

        String jsonPayload = """
                {
                  "adminUsername": "admin",
                  "action": "TEST_ACTION",
                  "entity": "TEST_ENTITY",
                  "description": "Running unit test for audit logs"
                }
                """;

        mockMvc.perform(post("/api/admin/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("TEST_ACTION"));
    }
}
