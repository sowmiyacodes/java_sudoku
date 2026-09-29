package com.sudoku.dto;

import java.util.List;

public record RecommendationResponseDto(
        Long userId,
        String skillLevel,
        double confidence,
        String recommendedDifficulty,
        String reason,
        String modelVersion,
        List<String> topFactors,
        boolean mlServiceAvailable
) {}
