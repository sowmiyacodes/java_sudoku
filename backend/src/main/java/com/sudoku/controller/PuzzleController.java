package com.sudoku.controller;

import com.sudoku.dto.GeneratePuzzleRequest;
import com.sudoku.dto.PuzzleResponse;
import com.sudoku.service.PuzzleProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzles")
public class PuzzleController {

    private final PuzzleProvider puzzleProvider;

    public PuzzleController(PuzzleProvider puzzleProvider) {
        this.puzzleProvider = puzzleProvider;
    }

    /**
     * Generate a new dynamic Sudoku puzzle without exposing the solution board.
     */
    @PostMapping("/generate")
    public ResponseEntity<PuzzleResponse> generatePuzzle(@RequestBody(required = false) GeneratePuzzleRequest request) {
        String difficulty = (request != null && request.getDifficulty() != null) ? request.getDifficulty() : "Medium";
        PuzzleProvider.Puzzle puzzle = puzzleProvider.getPuzzleByDifficulty(difficulty);

        int score = puzzle.metadata() != null ? puzzle.metadata().getScore() : 100;
        PuzzleResponse response = new PuzzleResponse(
                puzzle.id(),
                puzzle.difficulty(),
                score,
                puzzle.initialBoard(),
                puzzle.metadata()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/generate")
    public ResponseEntity<PuzzleResponse> generatePuzzleGet(@RequestParam(required = false, defaultValue = "Medium") String difficulty) {
        return generatePuzzle(new GeneratePuzzleRequest(difficulty));
    }
}
