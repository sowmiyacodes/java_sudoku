package com.sudoku;

import com.sudoku.dto.DifficultyAnalysis;
import com.sudoku.service.PuzzleProvider;
import com.sudoku.service.SudokuCandidateService;
import com.sudoku.service.SudokuDifficultyService;
import com.sudoku.service.SudokuSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class PuzzleGenerationTest {

    private PuzzleProvider puzzleProvider;
    private SudokuSolver solver;
    private SudokuDifficultyService difficultyService;

    @BeforeEach
    public void setup() {
        solver = new SudokuSolver();
        difficultyService = new SudokuDifficultyService(new SudokuCandidateService());
        puzzleProvider = new PuzzleProvider(solver, difficultyService);
    }

    @Test
    public void testGeneratedBoardIs9x9() {
        PuzzleProvider.Puzzle puzzle = puzzleProvider.generateDynamicPuzzle("Medium");
        assertEquals(9, puzzle.initialBoard().length);
        assertEquals(9, puzzle.initialBoard()[0].length);
        assertEquals(9, puzzle.solutionBoard().length);
        assertEquals(9, puzzle.solutionBoard()[0].length);
    }

    @Test
    public void testValuesAreValid() {
        PuzzleProvider.Puzzle puzzle = puzzleProvider.generateDynamicPuzzle("Medium");
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int val = puzzle.initialBoard()[r][c];
                assertTrue(val >= 0 && val <= 9, "Value should be between 0 and 9");
            }
        }
    }

    @Test
    public void testGeneratedSolutionIsValid() {
        PuzzleProvider.Puzzle puzzle = puzzleProvider.generateDynamicPuzzle("Hard");
        int[][] sol = puzzle.solutionBoard();
        assertTrue(solver.isBoardValid(sol), "Generated solution must be a valid Sudoku board");
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int val = sol[r][c];
                assertTrue(val >= 1 && val <= 9, "Solution cell must be 1-9");
            }
        }
    }

    @Test
    public void testPuzzleHasExactlyOneSolution() {
        PuzzleProvider.Puzzle puzzle = puzzleProvider.generateDynamicPuzzle("Hard");
        int[][] boardCopy = new int[9][9];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(puzzle.initialBoard()[i], 0, boardCopy[i], 0, 9);
        }
        int count = solver.countSolutions(boardCopy, 2);
        assertEquals(1, count, "Generated puzzle must have exactly ONE unique solution");
    }

    @Test
    public void testGeneratedPuzzleDiffersBetweenGenerations() {
        PuzzleProvider.Puzzle p1 = puzzleProvider.generateDynamicPuzzle("Medium");
        PuzzleProvider.Puzzle p2 = puzzleProvider.generateDynamicPuzzle("Medium");
        assertFalse(Arrays.deepEquals(p1.solutionBoard(), p2.solutionBoard()), "Two generated puzzles should be randomized");
    }

    @Test
    public void testEasyGeneration() {
        PuzzleProvider.Puzzle easy = puzzleProvider.generateDynamicPuzzle("Easy");
        assertNotNull(easy);
        assertEquals(1, solver.countSolutions(easy.initialBoard(), 2));
        assertTrue(easy.metadata().getEmptyCells() <= 40, "Easy should have fewer empty cells than hard");
    }

    @Test
    public void testMediumGeneration() {
        PuzzleProvider.Puzzle med = puzzleProvider.generateDynamicPuzzle("Medium");
        assertNotNull(med);
        assertEquals(1, solver.countSolutions(med.initialBoard(), 2));
    }

    @Test
    public void testHardGeneration() {
        PuzzleProvider.Puzzle hard = puzzleProvider.generateDynamicPuzzle("Hard");
        assertNotNull(hard);
        assertEquals(1, solver.countSolutions(hard.initialBoard(), 2));
        assertTrue(hard.metadata().getEmptyCells() >= 45, "Hard should have higher empty cell count");
    }

    @Test
    public void testExpertGeneration() {
        PuzzleProvider.Puzzle expert = puzzleProvider.generateDynamicPuzzle("Expert");
        assertNotNull(expert);
        assertEquals(1, solver.countSolutions(expert.initialBoard(), 2));
        assertTrue(expert.metadata().getEmptyCells() >= 50, "Expert should have high empty cell count");
    }

    @Test
    public void testDifficultyAnalysisMetadata() {
        PuzzleProvider.Puzzle p = puzzleProvider.generateDynamicPuzzle("Hard");
        DifficultyAnalysis meta = p.metadata();
        assertNotNull(meta);
        assertTrue(meta.getEmptyCells() > 0);
        assertTrue(meta.getScore() > 0);
        assertTrue(meta.getCandidateCount() > 0);
        assertNotNull(meta.getTechniquesUsed());
        assertFalse(meta.getTechniquesUsed().isEmpty());
    }
}
