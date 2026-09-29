package com.sudoku.repository;

import com.sudoku.model.Puzzle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PuzzleRepository extends JpaRepository<Puzzle, Long> {
    Optional<Puzzle> findByPuzzleId(String puzzleId);
    Optional<Puzzle> findByPuzzleIdAndActiveTrue(String puzzleId);
    List<Puzzle> findByActiveTrueOrderByCreatedAtDesc();
    List<Puzzle> findByDifficultyIgnoreCaseAndActiveTrue(String difficulty);
}