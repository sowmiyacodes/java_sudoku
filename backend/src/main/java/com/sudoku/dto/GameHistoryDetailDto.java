package com.sudoku.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GameHistoryDetailDto(
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
        String completionStatus,
        List<MoveResponse> recentMoves,
        List<HintHistoryResponse> hintHistory
) {}
