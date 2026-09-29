package com.sudoku;

import com.sudoku.model.User;
import com.sudoku.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PlayerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testpilot");
        testUser.setDisplayName("Test Pilot");
        testUser.setEmail("testpilot@example.com");
        testUser.setPasswordHash("hash");
        userRepository.findByUsernameIgnoreCase("testpilot")
                .ifPresent(existing -> userRepository.delete(existing));
        testUser = userRepository.save(testUser);
    }

    @Test
    void testUnauthenticatedProfileAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/players/me/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testpilot", roles = "USER")
    void testAuthenticatedProfileAccessReturns200() throws Exception {
        mockMvc.perform(get("/api/players/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testpilot"))
                .andExpect(jsonPath("$.gamesPlayed").value(0))
                .andExpect(jsonPath("$.recommendedDifficulty").value("Easy"));
    }

    @Test
    @WithMockUser(username = "testpilot", roles = "USER")
    void testAuthenticatedStatisticsAccessReturns200() throws Exception {
        mockMvc.perform(get("/api/players/me/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesPlayed").value(0))
                .andExpect(jsonPath("$.completionRate").value(0.0));
    }

    @Test
    @WithMockUser(username = "testpilot", roles = "USER")
    void testAuthenticatedRecommendationsAccessReturns200() throws Exception {
        mockMvc.perform(get("/api/players/me/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedDifficulty").isNotEmpty())
                .andExpect(jsonPath("$.skillLevel").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testpilot", roles = "USER")
    void testAuthenticatedGameHistoryAccessReturns200() throws Exception {
        mockMvc.perform(get("/api/players/me/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
