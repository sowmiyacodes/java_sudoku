package com.sudoku.dto;

public record PuzzleManagementRequest(
        String puzzle,
        String difficulty,
        String source,
        Double rating,
        Boolean active
) {}