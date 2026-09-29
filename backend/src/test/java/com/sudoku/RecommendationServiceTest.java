package com.sudoku;

import com.sudoku.dto.RecommendationResponseDto;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.Recommendation;
import com.sudoku.model.User;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.RecommendationRepository;
import com.sudoku.repository.UserRepository;
import com.sudoku.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RecommendationServiceTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("rec_tester_" + System.currentTimeMillis());
        testUser.setDisplayName("Recommendation Tester");
        testUser.setEmail("rec_tester_" + System.currentTimeMillis() + "@example.com");
        testUser.setPasswordHash("hashed_pwd");
        testUser = userRepository.save(testUser);
    }

    @Test
    void testRecommendationForNewUserRecommendsEasy() {
        RecommendationResponseDto rec = recommendationService.generateRecommendation(testUser.getId());
        assertNotNull(rec);
        assertEquals("Easy", rec.recommendedDifficulty());
        assertNotNull(rec.reason());
        assertTrue(rec.reason().contains("Welcome"));

        List<Recommendation> saved = recommendationRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertFalse(saved.isEmpty());
        assertEquals("Easy", saved.get(0).getRecommendedDifficulty());
    }

    @Test
    void testRecommendationPersistsInH2() {
        Game game = new Game();
        game.setUser(testUser);
        game.setDifficulty("Medium");
        game.setStatus(GameStatus.COMPLETED);
        game.setElapsedSeconds(400);
        game.setInitialBoardJson("[[0]]");
        game.setCurrentBoardJson("[[1]]");
        gameRepository.save(game);

        RecommendationResponseDto rec = recommendationService.generateRecommendation(testUser.getId());
        assertNotNull(rec);
        assertNotNull(rec.skillLevel());
        assertNotNull(rec.recommendedDifficulty());

        RecommendationResponseDto latest = recommendationService.getLatestRecommendation(testUser.getId());
        assertEquals(rec.recommendedDifficulty(), latest.recommendedDifficulty());
    }
}
