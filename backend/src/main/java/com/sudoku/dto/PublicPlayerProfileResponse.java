package com.sudoku.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicPlayerProfileResponse(
        Long userId,
        String username,
        String displayName,
        LocalDateTime memberSince,
        Integer allTimeRank,
        long totalPoints,
        long gamesCompleted,
        double averageAccuracy,
        long bestTime,
        List<ScoreHistoryDto> recentScores
) {}
