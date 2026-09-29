package com.sudoku.dto;

import com.sudoku.model.Puzzle;

import java.time.LocalDateTime;

public record PuzzleRecordResponse(
        Long id,
        String puzzleId,
        String puzzle,
        String difficulty,
        String predictedDifficulty,
        Integer difficultyScore,
        Double modelConfidence,
        String source,
        Double rating,
        String featuresJson,
        String topFactorsJson,
        boolean active,
        LocalDateTime createdAt
) {
    public static PuzzleRecordResponse from(Puzzle puzzle) {
        return new PuzzleRecordResponse(
                puzzle.getId(), puzzle.getPuzzleId(), puzzle.getPuzzle(), puzzle.getDifficulty(),
                puzzle.getPredictedDifficulty(), puzzle.getDifficultyScore(), puzzle.getModelConfidence(),
                puzzle.getSource(), puzzle.getRating(), puzzle.getFeaturesJson(), puzzle.getTopFactorsJson(),
                puzzle.isActive(), puzzle.getCreatedAt()
        );
    }
}