package com.sudoku.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SkillPredictionResponseDto(
        @JsonProperty("skill_level") String skillLevel,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("explanation") ExplanationDto explanation
) {
    public record ExplanationDto(
            @JsonProperty("top_factors") List<String> topFactors
    ) {}

    public static SkillPredictionResponseDto fallback() {
        return new SkillPredictionResponseDto(
                "INTERMEDIATE",
                0.50,
                "fallback",
                new ExplanationDto(List.of("Default baseline assessment"))
        );
    }
}
