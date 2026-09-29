package com.sudoku.service;

import com.sudoku.dto.RecommendationResponseDto;
import com.sudoku.dto.SkillPredictionRequestDto;
import com.sudoku.dto.SkillPredictionResponseDto;
import com.sudoku.model.PlayerPreference;
import com.sudoku.model.PlayerStatistics;
import com.sudoku.model.Recommendation;
import com.sudoku.repository.PlayerPreferenceRepository;
import com.sudoku.repository.PlayerStatisticsRepository;
import com.sudoku.repository.RecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {

    private final PlayerStatisticsService statisticsService;
    private final MLPredictionService mlPredictionService;
    private final RecommendationRepository recommendationRepository;
    private final PlayerPreferenceRepository preferenceRepository;
    private final PlayerStatisticsRepository statisticsRepository;

    public RecommendationService(
            PlayerStatisticsService statisticsService,
            MLPredictionService mlPredictionService,
            RecommendationRepository recommendationRepository,
            PlayerPreferenceRepository preferenceRepository,
            PlayerStatisticsRepository statisticsRepository
    ) {
        this.statisticsService = statisticsService;
        this.mlPredictionService = mlPredictionService;
        this.recommendationRepository = recommendationRepository;
        this.preferenceRepository = preferenceRepository;
        this.statisticsRepository = statisticsRepository;
    }

    /**
     * Generates a personalized puzzle recommendation for a player by:
     * 1. Loading latest player statistics from H2.
     * 2. Querying the ML model for predicted skill.
     * 3. Analyzing empirical difficulty completion rates and error trends to recommend next difficulty.
     * 4. Persisting the recommendation to H2.
     */
    @Transactional
    public RecommendationResponseDto generateRecommendation(Long userId) {
        PlayerStatistics stats = statisticsRepository.findByUserId(userId)
                .orElseGet(() -> statisticsService.calculateAndSaveStatistics(userId));

        SkillPredictionRequestDto payload = statisticsService.buildPredictionPayload(stats);
        SkillPredictionResponseDto prediction = mlPredictionService.predictSkill(userId, payload);

        boolean mlAvailable = !"offline-rule-heuristic".equalsIgnoreCase(prediction.modelVersion());

        // Recommendation Logic
        String skill = prediction.skillLevel();
        String recommendedDiff;
        String reason;

        if (stats.getGamesPlayed() == 0) {
            recommendedDiff = "Easy";
            reason = "Welcome to Sudoku! Start with Easy puzzles to establish your baseline and get comfortable with board mechanics.";
        } else if ("EXPERT".equalsIgnoreCase(skill)) {
            if (stats.getHardCompletionRate() >= 0.85) {
                recommendedDiff = "Hard";
                reason = String.format("Outstanding master performance! Your Hard puzzle completion rate is %d%% with %.1fs average time. Maintain your speed on Hard missions.",
                        (int) (stats.getHardCompletionRate() * 100), (double) stats.getAverageTime());
            } else {
                recommendedDiff = "Hard";
                reason = "Your overall skill profile is Expert. Focus on Hard puzzles to push your speed and solve consistency without relying on hints.";
            }
        } else if ("ADVANCED".equalsIgnoreCase(skill)) {
            if (stats.getHardCompletionRate() >= 0.65) {
                recommendedDiff = "Hard";
                reason = String.format("Strong progress! You have completed %d%% of Hard puzzles. Continue practicing Hard to refine complex deductions.",
                        (int) (stats.getHardCompletionRate() * 100));
            } else {
                recommendedDiff = "Medium";
                reason = String.format("Your Medium completion rate is solid (%d%%), but Hard puzzles still pose a challenge (%d%% completed). Cement your foundation on Medium before tackling Hard.",
                        (int) (stats.getMediumCompletionRate() * 100), (int) (stats.getHardCompletionRate() * 100));
            }
        } else if ("INTERMEDIATE".equalsIgnoreCase(skill)) {
            if (stats.getMediumCompletionRate() >= 0.70) {
                recommendedDiff = "Medium";
                reason = String.format("Good consistency on Medium puzzles (%d%% completion). Work on minimizing mistakes (%s avg) to prepare for Hard.",
                        (int) (stats.getMediumCompletionRate() * 100), stats.getAverageMistakes());
            } else {
                recommendedDiff = "Easy";
                reason = String.format("Your accuracy is %.1f%%. Completing more Easy puzzles with zero mistakes will build speed and deduction confidence.",
                        stats.getAverageAccuracy() * 100);
            }
        } else { // BEGINNER
            recommendedDiff = "Easy";
            if (stats.getAverageHints() > 2.0) {
                reason = String.format("You are averaging %.1f hints per game. Practice Easy puzzles with candidates enabled to build candidate elimination skills.",
                        stats.getAverageHints());
            } else {
                reason = "Focus on Easy puzzles to develop scanning techniques and eliminate row/column conflicts.";
            }
        }

        // Persist Recommendation in H2
        Recommendation recommendation = new Recommendation(
                userId,
                recommendedDiff,
                reason,
                prediction.confidence(),
                skill,
                prediction.modelVersion()
        );
        recommendationRepository.save(recommendation);

        // Update target difficulty preference
        PlayerPreference pref = preferenceRepository.findByUserId(userId)
                .orElse(new PlayerPreference(userId));
        pref.setPreferredDifficulty(recommendedDiff);
        pref.setTargetDifficulty(recommendedDiff.equalsIgnoreCase("Easy") ? "Medium" : "Hard");
        preferenceRepository.save(pref);

        List<String> factors = new ArrayList<>();
        if (prediction.explanation() != null && prediction.explanation().topFactors() != null) {
            factors.addAll(prediction.explanation().topFactors());
        }

        return new RecommendationResponseDto(
                userId,
                skill,
                prediction.confidence(),
                recommendedDiff,
                reason,
                prediction.modelVersion(),
                factors,
                mlAvailable
        );
    }

    @Transactional(readOnly = true)
    public RecommendationResponseDto getLatestRecommendation(Long userId) {
        return recommendationRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(r -> new RecommendationResponseDto(
                        r.getUserId(),
                        r.getSkillLevel() != null ? r.getSkillLevel() : "INTERMEDIATE",
                        r.getConfidence(),
                        r.getRecommendedDifficulty(),
                        r.getReason(),
                        r.getModelVersion(),
                        List.of("Historical recommendation profile"),
                        true
                ))
                .orElseGet(() -> generateRecommendation(userId));
    }
}
