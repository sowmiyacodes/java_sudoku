package com.sudoku.dto;

import com.sudoku.model.LeaderboardScore;
import java.time.LocalDateTime;

public record ScoreHistoryDto(
        Long gameId,
        String difficulty,
        int points,
        int mistakes,
        int hintsUsed,
        long elapsedSeconds,
        int accuracy,
        LocalDateTime completedAt
) {
    public static ScoreHistoryDto fromEntity(LeaderboardScore score) {
        return new ScoreHistoryDto(
                score.getGame().getId(),
                score.getDifficulty(),
                score.getPoints(),
                score.getMistakes(),
                score.getHintsUsed(),
                score.getElapsedSeconds(),
                score.getAccuracy(),
                score.getCompletedAt()
        );
    }
}
