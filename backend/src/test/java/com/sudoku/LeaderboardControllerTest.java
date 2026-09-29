package com.sudoku;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudoku.dto.RegisterRequest;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.User;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.UserRepository;
import com.sudoku.service.GameService;
import com.sudoku.service.ScoringService;
import com.sudoku.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sudoku-leaderboard-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameService gameService;

    @Autowired
    private ScoringService scoringService;

    private User registeredUser;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest registerReq = new RegisterRequest(
                "pilot_" + unique,
                "Pilot " + unique,
                "pilot_" + unique + "@sudoku.com",
                "Password123!"
        );
        registeredUser = userService.register(registerReq);
    }

    @Test
    @DisplayName("Public leaderboard endpoint is accessible without authentication")
    void testGetLeaderboard_Public() throws Exception {
        mockMvc.perform(get("/api/leaderboard?period=all-time"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("My rank endpoint rejects unauthenticated requests")
    void testGetMyRank_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/leaderboard/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("My history endpoint rejects unauthenticated requests")
    void testGetMyHistory_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/leaderboard/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Public profile endpoint returns player stats and omits private email")
    void testGetPublicPlayerProfile() throws Exception {
        mockMvc.perform(get("/api/leaderboard/profile/" + registeredUser.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(registeredUser.getUsername()))
                .andExpect(jsonPath("$.displayName").value(registeredUser.getDisplayName()))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("Private game cannot be accessed by another user")
    void testPrivateGameAccessControl() {
        // Create game belonging to registeredUser
        var createResponse = gameService.createGame(null, registeredUser);
        Long gameId = createResponse.getId();

        // Create second user
        String unique2 = UUID.randomUUID().toString().substring(0, 8);
        User otherUser = userService.register(new RegisterRequest(
                "other_" + unique2,
                "Other " + unique2,
                "other_" + unique2 + "@sudoku.com",
                "Password123!"
        ));

        // Registered user CAN access
        assertDoesNotThrow(() -> gameService.getGame(gameId, registeredUser));

        // Other user CANNOT access -> throws 403 FORBIDDEN
        org.springframework.web.server.ResponseStatusException ex =
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> gameService.getGame(gameId, otherUser));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatusCode());

        // Anonymous user CANNOT access private game -> throws 403 FORBIDDEN
        org.springframework.web.server.ResponseStatusException anonEx =
                assertThrows(org.springframework.web.server.ResponseStatusException.class,
                        () -> gameService.getGame(gameId, null));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, anonEx.getStatusCode());
    }

    private void assertDoesNotThrow(Runnable action) {
        action.run();
    }
}
