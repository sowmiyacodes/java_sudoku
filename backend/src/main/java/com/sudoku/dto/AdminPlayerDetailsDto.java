package com.sudoku.dto;

import java.util.List;

public record AdminPlayerDetailsDto(
        PlayerProfileDto profile,
        RecommendationResponseDto recommendation,
        List<DifficultyPerformanceDto> difficultyPerformance
) {}
