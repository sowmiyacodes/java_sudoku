package com.sudoku;

import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.LeaderboardScore;
import com.sudoku.model.SudokuBoard;
import com.sudoku.model.User;
import com.sudoku.repository.HintHistoryRepository;
import com.sudoku.repository.LeaderboardScoreRepository;
import com.sudoku.repository.MoveRepository;
import com.sudoku.service.ScoringService;
import com.sudoku.service.SudokuValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScoringServiceTest {

    @Mock
    private LeaderboardScoreRepository leaderboardScoreRepository;

    @Mock
    private MoveRepository moveRepository;

    @Mock
    private HintHistoryRepository hintHistoryRepository;

    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private SudokuValidationService validationService;
    private ScoringService scoringService;

    private static final int[][] VALID_SOLVED_BOARD = {
            {5, 3, 4, 6, 7, 8, 9, 1, 2},
            {6, 7, 2, 1, 9, 5, 3, 4, 8},
            {1, 9, 8, 3, 4, 2, 5, 6, 7},
            {8, 5, 9, 7, 6, 1, 4, 2, 3},
            {4, 2, 6, 8, 5, 3, 7, 9, 1},
            {7, 1, 3, 9, 2, 4, 8, 5, 6},
            {9, 6, 1, 5, 3, 7, 2, 8, 4},
            {2, 8, 7, 4, 1, 9, 6, 3, 5},
            {3, 4, 5, 2, 8, 6, 1, 7, 9}
    };

    @BeforeEach
    void setUp() {
        validationService = new SudokuValidationService();
        scoringService = new ScoringService(
                leaderboardScoreRepository,
                moveRepository,
                hintHistoryRepository,
                validationService,
                transactionManager
        );
    }

    @Test
    @DisplayName("Formula calculates standard score with speed bonus for fast completion")
    void testCalculatePoints_WithTimeBonus() {
        // Medium: Base 1000, target 600s. Finished in 400s (200s saved * 1.5 = 300 bonus)
        // 1 mistake * 60 = 60 penalty, 1 hint * 50 = 50 penalty.
        // Expected: 1000 + 300 - 60 - 50 = 1190
        int score = scoringService.calculatePoints("Medium", 1, 1, 400);
        assertEquals(1190, score);
    }

    @Test
    @DisplayName("Formula applies time penalty decay for slow completion")
    void testCalculatePoints_WithTimePenalty() {
        // Easy: Base 500, target 300s. Finished in 400s (100s extra * 0.5 = 50 penalty)
        // 0 mistakes, 0 hints.
        // Expected: 500 - 50 = 450
        int score = scoringService.calculatePoints("Easy", 0, 0, 400);
        assertEquals(450, score);
    }

    @Test
    @DisplayName("Formula caps time bonus at 50% base points")
    void testCalculatePoints_TimeBonusCapped() {
        // Hard: Base 1800, target 900s. Finished in 10s (890s saved * 2.0 = 1780 bonus, but capped at 900)
        // Expected: 1800 + 900 = 2700
        int score = scoringService.calculatePoints("Hard", 0, 0, 10);
        assertEquals(2700, score);
    }

    @Test
    @DisplayName("Formula prevents negative scores under high mistakes and hints")
    void testCalculatePoints_NeverNegative() {
        // Easy: Base 500. 20 mistakes * 40 = 800 penalty. 10 hints * 30 = 300 penalty. Extra time 1000s.
        // Raw score is negative, must be clamped to 0.
        int score = scoringService.calculatePoints("Easy", 20, 10, 2000);
        assertEquals(0, score);
    }

    @Test
    @DisplayName("Accuracy calculation returns correct percentage")
    void testCalculateAccuracy() {
        assertEquals(100, scoringService.calculateAccuracy(0, 0));
        assertEquals(100, scoringService.calculateAccuracy(45, 0));
        assertEquals(90, scoringService.calculateAccuracy(45, 5)); // 45 / 50 = 90%
        assertEquals(80, scoringService.calculateAccuracy(40, 10)); // 40 / 50 = 80%
    }

    @Test
    @DisplayName("awardScore successfully awards points after game completion validation")
    void testAwardScore_Success() {
        User user = new User();
        user.setUsername("pilot1");

        Game game = new Game();
        game.setId(10L);
        game.setDifficulty("Medium");
        game.setStatus(GameStatus.COMPLETED);
        game.setElapsedSeconds(300);
        game.setMistakes(0);
        game.setCompletedAt(LocalDateTime.now());
        game.setCurrentBoardJson(new SudokuBoard(VALID_SOLVED_BOARD).toJson());

        when(leaderboardScoreRepository.existsByGameIdAndUserId(10L, user.getId())).thenReturn(false);
        when(hintHistoryRepository.countByGameId(10L)).thenReturn(0L);
        when(moveRepository.countByGameId(10L)).thenReturn(40L);
        when(leaderboardScoreRepository.saveAndFlush(any(LeaderboardScore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaderboardScore awarded = scoringService.awardScore(game, user);

        assertNotNull(awarded);
        assertEquals(user, awarded.getUser());
        assertEquals(game, awarded.getGame());
        assertTrue(awarded.getPoints() > 0);
        verify(leaderboardScoreRepository, times(1)).saveAndFlush(any(LeaderboardScore.class));
    }

    @Test
    @DisplayName("awardScore rejects game when validation does not pass")
    void testAwardScore_RejectsIncompleteOrUnsolvedGame() {
        User user = new User();
        user.setUsername("pilot1");

        Game game = new Game();
        game.setId(20L);
        game.setStatus(GameStatus.IN_PROGRESS); // Not completed
        game.setCurrentBoardJson(new SudokuBoard().toJson()); // Empty board

        LeaderboardScore awarded = scoringService.awardScore(game, user);
        assertNull(awarded);
        verify(leaderboardScoreRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("awardScore prevents duplicate awards for already scored game")
    void testAwardScore_PreventsDuplicateAward() {
        User user = new User();
        user.setUsername("pilot1");

        Game game = new Game();
        game.setId(30L);
        game.setStatus(GameStatus.COMPLETED);

        LeaderboardScore existingScore = new LeaderboardScore();
        existingScore.setPoints(1200);

        when(leaderboardScoreRepository.existsByGameIdAndUserId(30L, user.getId())).thenReturn(true);
        when(leaderboardScoreRepository.findByGameIdAndUserId(30L, user.getId())).thenReturn(Optional.of(existingScore));

        LeaderboardScore result = scoringService.awardScore(game, user);

        assertNotNull(result);
        assertEquals(1200, result.getPoints());
        verify(leaderboardScoreRepository, never()).saveAndFlush(any());
    }
}
