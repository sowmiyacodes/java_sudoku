package com.sudoku;

import com.sudoku.dto.DifficultyPerformanceDto;
import com.sudoku.dto.PlayerStatisticsDto;
import com.sudoku.model.*;
import com.sudoku.repository.*;
import com.sudoku.service.PlayerStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PlayerStatisticsServiceTest {

    @Autowired
    private PlayerStatisticsService statisticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private MoveRepository moveRepository;

    @Autowired
    private PlayerStatisticsRepository statisticsRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("stat_tester_" + System.currentTimeMillis());
        testUser.setDisplayName("Stat Tester");
        testUser.setEmail("stat_tester_" + System.currentTimeMillis() + "@example.com");
        testUser.setPasswordHash("hashed_pwd");
        testUser = userRepository.save(testUser);
    }

    @Test
    void testEmptyHistoryGeneratesZeroStatistics() {
        PlayerStatistics stats = statisticsService.calculateAndSaveStatistics(testUser.getId());
        assertNotNull(stats);
        assertEquals(0, stats.getGamesPlayed());
        assertEquals(0, stats.getGamesCompleted());
        assertEquals(0.0, stats.getCompletionRate());
        assertEquals(0, stats.getAverageTime());

        PlayerStatisticsDto dto = statisticsService.getPlayerStatistics(testUser.getId());
        assertEquals(0, dto.gamesPlayed());
        assertEquals(0.0, dto.completionRate());
        assertEquals(0, dto.currentStreak());
        assertEquals(0, dto.bestStreak());
    }

    @Test
    void testSingleGameCalculatesAccurateFeatures() {
        Game game = new Game();
        game.setUser(testUser);
        game.setDifficulty("Medium");
        game.setStatus(GameStatus.COMPLETED);
        game.setElapsedSeconds(420);
        game.setMistakes(2);
        game.setInitialBoardJson("[[0]]");
        game.setCurrentBoardJson("[[1]]");
        game = gameRepository.save(game);

        // Add 10 moves
        for (int i = 1; i <= 10; i++) {
            Move m = new Move(game.getId(), 0, i % 9, (i % 9) + 1, 0, i);
            moveRepository.save(m);
        }

        PlayerStatistics stats = statisticsService.calculateAndSaveStatistics(testUser.getId());
        assertNotNull(stats);
        assertEquals(1, stats.getGamesPlayed());
        assertEquals(1, stats.getGamesCompleted());
        assertEquals(1.0, stats.getCompletionRate());
        assertEquals(420, stats.getAverageTime());
        assertEquals(420, stats.getBestTime());
        assertEquals(2.0, stats.getAverageMistakes());
        assertEquals(10.0, stats.getAverageMoves());
        assertEquals(1.0, stats.getMediumCompletionRate());
        assertEquals(0.0, stats.getHardCompletionRate());

        // Accuracy: 10 moves / (10 moves + 2 mistakes) = 10 / 12 = 0.8333
        assertTrue(stats.getAverageAccuracy() >= 0.83 && stats.getAverageAccuracy() <= 0.84);
    }

    @Test
    void testDifficultyPerformanceBreakdown() {
        // Create 1 Easy completed, 1 Medium in-progress
        Game g1 = new Game();
        g1.setUser(testUser);
        g1.setDifficulty("Easy");
        g1.setStatus(GameStatus.COMPLETED);
        g1.setElapsedSeconds(180);
        g1.setInitialBoardJson("[[0]]");
        g1.setCurrentBoardJson("[[1]]");
        gameRepository.save(g1);

        Game g2 = new Game();
        g2.setUser(testUser);
        g2.setDifficulty("Medium");
        g2.setStatus(GameStatus.IN_PROGRESS);
        g2.setElapsedSeconds(300);
        g2.setInitialBoardJson("[[0]]");
        g2.setCurrentBoardJson("[[0]]");
        gameRepository.save(g2);

        List<DifficultyPerformanceDto> performance = statisticsService.getDifficultyPerformance(testUser.getId());
        assertEquals(3, performance.size());

        DifficultyPerformanceDto easy = performance.stream().filter(p -> p.difficulty().equalsIgnoreCase("Easy")).findFirst().orElseThrow();
        assertEquals(1, easy.games());
        assertEquals(1, easy.completed());
        assertEquals(1.0, easy.completionRate());
        assertEquals(180, easy.averageTime());

        DifficultyPerformanceDto medium = performance.stream().filter(p -> p.difficulty().equalsIgnoreCase("Medium")).findFirst().orElseThrow();
        assertEquals(1, medium.games());
        assertEquals(0, medium.completed());
        assertEquals(0.0, medium.completionRate());

        DifficultyPerformanceDto hard = performance.stream().filter(p -> p.difficulty().equalsIgnoreCase("Hard")).findFirst().orElseThrow();
        assertEquals(0, hard.games());
        assertEquals(0, hard.completed());
    }
}
