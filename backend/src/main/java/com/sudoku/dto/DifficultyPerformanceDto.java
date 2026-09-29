package com.sudoku.dto;

public record DifficultyPerformanceDto(
        String difficulty,
        int games,
        int completed,
        double completionRate,
        long averageTime,
        double accuracy
) {}
