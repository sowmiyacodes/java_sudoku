package com.sudoku;

import com.sudoku.service.MLPredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminAndPlayerMLControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MLPredictionService mlService;

    @Test
    void testAdminOverviewEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/ml/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HEALTHY"))
                .andExpect(jsonPath("$.total_models").exists())
                .andExpect(jsonPath("$.total_training_runs").exists());
    }

    @Test
    void testAdminModelsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/ml/models"))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminExperimentsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/ml/experiments"))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminDatasetsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/ml/datasets"))
                .andExpect(status().isOk());
    }

    @Test
    void testPlayerPredictDifficulty() throws Exception {
        String puzzleJson = "{\"puzzle\":\"53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79\"}";
        mockMvc.perform(post("/api/ml/predict/difficulty")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(puzzleJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").exists());
    }

    @Test
    void testPlayerPredictCompletion() throws Exception {
        String reqJson = "{\"skill_level\":\"ADVANCED\",\"difficulty\":\"MEDIUM\",\"recent_accuracy\":0.95,\"current_progress\":0.60}";
        mockMvc.perform(post("/api/ml/predict/completion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completion_probability").exists());
    }

    @Test
    void testPlayerPredictHint() throws Exception {
        String reqJson = "{\"player_skill\":\"INTERMEDIATE\",\"difficulty\":\"HARD\",\"current_progress\":0.40}";
        mockMvc.perform(post("/api/ml/predict/hint")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hint_type").exists());
    }
}
