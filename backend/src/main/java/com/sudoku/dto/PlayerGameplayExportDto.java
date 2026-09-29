package com.sudoku.dto;

import java.time.LocalDateTime;

public record PlayerGameplayExportDto(
        Long gameId,
        Long playerId,
        String difficulty,
        long duration,
        int moves,
        int mistakes,
        int hints,
        int undos,
        double accuracy,
        int score,
        String completionStatus,
        LocalDateTime timestamp
) {}
