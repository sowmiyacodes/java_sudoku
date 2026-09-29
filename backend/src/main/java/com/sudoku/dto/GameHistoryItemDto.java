package com.sudoku.dto;

import java.time.LocalDateTime;

public record GameHistoryItemDto(
        Long gameId,
        String puzzleId,
        String difficulty,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        long duration,
        int moves,
        int mistakes,
        int hints,
        int undos,
        double accuracy,
        int score,
        String completionStatus
) {}
