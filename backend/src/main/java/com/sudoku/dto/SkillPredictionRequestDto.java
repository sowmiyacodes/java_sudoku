package com.sudoku.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SkillPredictionRequestDto(
        @JsonProperty("games_played") int gamesPlayed,
        @JsonProperty("completion_rate") double completionRate,
        @JsonProperty("average_time") double averageTime,
        @JsonProperty("average_score") double averageScore,
        @JsonProperty("average_accuracy") double averageAccuracy,
        @JsonProperty("average_mistakes") double averageMistakes,
        @JsonProperty("average_hints") double averageHints,
        @JsonProperty("average_undos") double averageUndos,
        @JsonProperty("average_moves") double averageMoves,
        @JsonProperty("hard_completion_rate") double hardCompletionRate,
        @JsonProperty("medium_completion_rate") double mediumCompletionRate,
        @JsonProperty("easy_completion_rate") double easyCompletionRate,
        @JsonProperty("recent_accuracy") double recentAccuracy,
        @JsonProperty("recent_error_rate") double recentErrorRate,
        @JsonProperty("average_moves_per_minute") double averageMovesPerMinute
) {}
