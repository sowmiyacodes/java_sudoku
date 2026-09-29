package com.sudoku;

import com.sudoku.dto.PlayerProfileDto;
import com.sudoku.dto.UpdateProfileRequest;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.PlayerStatistics;
import com.sudoku.model.User;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.UserRepository;
import com.sudoku.service.PlayerStatisticsService;
import com.sudoku.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PlayerCrudAndStreakTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerStatisticsService statisticsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("crud_player_" + System.currentTimeMillis());
        testUser.setDisplayName("Original Name");
        testUser.setEmail("crud_player_" + System.currentTimeMillis() + "@example.com");
        testUser.setPasswordHash("hashed_pwd");
        testUser = userRepository.save(testUser);
    }

    @Test
    void testUpdateAndGetPlayerProfile() {
        UpdateProfileRequest updateReq = new UpdateProfileRequest("Updated Name", "updated_" + System.currentTimeMillis() + "@example.com");
        User updated = userService.updateProfile(testUser.getUsername(), updateReq);

        assertNotNull(updated);
        assertEquals("Updated Name", updated.getDisplayName());
        assertEquals(updateReq.email().toLowerCase(), updated.getEmail());
    }

    @Test
    void testDeletePlayerProfile() {
        Long userId = testUser.getId();
        assertTrue(userRepository.existsById(userId));

        userService.deleteUser(userId);
        assertFalse(userRepository.existsById(userId));
    }

    @Test
    void testStreakCalculationConsecutiveGames() {
        // Create 3 completed games in a row
        Game g1 = new Game();
        g1.setUser(testUser);
        g1.setDifficulty("Easy");
        g1.setStatus(GameStatus.COMPLETED);
        g1.setElapsedSeconds(200);
        g1.setInitialBoardJson("[[0]]");
        g1.setCurrentBoardJson("[[1]]");
        gameRepository.save(g1);

        Game g2 = new Game();
        g2.setUser(testUser);
        g2.setDifficulty("Medium");
        g2.setStatus(GameStatus.COMPLETED);
        g2.setElapsedSeconds(350);
        g2.setInitialBoardJson("[[0]]");
        g2.setCurrentBoardJson("[[1]]");
        gameRepository.save(g2);

        Game g3 = new Game();
        g3.setUser(testUser);
        g3.setDifficulty("Hard");
        g3.setStatus(GameStatus.COMPLETED);
        g3.setElapsedSeconds(500);
        g3.setInitialBoardJson("[[0]]");
        g3.setCurrentBoardJson("[[1]]");
        gameRepository.save(g3);

        PlayerStatistics stats = statisticsService.calculateAndSaveStatistics(testUser.getId());
        assertEquals(3, stats.getGamesPlayed());
        assertEquals(3, stats.getGamesCompleted());
        assertEquals(3, stats.getCurrentStreak());
        assertEquals(3, stats.getBestStreak());

        // Now add an abandoned game (breaks current streak)
        Game g4 = new Game();
        g4.setUser(testUser);
        g4.setDifficulty("Hard");
        g4.setStatus(GameStatus.IN_PROGRESS);
        g4.setElapsedSeconds(100);
        g4.setInitialBoardJson("[[0]]");
        g4.setCurrentBoardJson("[[0]]");
        gameRepository.save(g4);

        PlayerStatistics statsAfterBreak = statisticsService.calculateAndSaveStatistics(testUser.getId());
        assertEquals(4, statsAfterBreak.getGamesPlayed());
        assertEquals(0, statsAfterBreak.getCurrentStreak());
        assertEquals(3, statsAfterBreak.getBestStreak());
    }
}
