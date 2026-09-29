package com.sudoku.dto;

import java.time.LocalDateTime;

public record PlayerProfileDto(
        Long userId,
        String username,
        String displayName,
        String email,
        LocalDateTime registrationDate,
        int gamesPlayed,
        int gamesCompleted,
        int gamesAbandoned,
        double completionRate,
        long averageSolvingTime,
        long bestSolvingTime,
        double averageAccuracy,
        int totalMistakes,
        double averageMistakesPerGame,
        double averageHintsPerGame,
        double averageUndosPerGame,
        int averageScore,
        int currentStreak,
        int bestStreak,
        String currentSkillLevel,
        double skillConfidence,
        String recommendedDifficulty,
        String recommendationReason
) {}
