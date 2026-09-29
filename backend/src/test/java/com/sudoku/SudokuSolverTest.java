package com.sudoku;

import com.sudoku.service.SudokuSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SudokuSolverTest {

    private SudokuSolver solver;

    @BeforeEach
    public void setup() {
        solver = new SudokuSolver();
    }

    // Classic valid, solvable Sudoku puzzle
    private int[][] createSolvablePuzzle() {
        return new int[][] {
            {5, 3, 0, 0, 7, 0, 0, 0, 0},
            {6, 0, 0, 1, 9, 5, 0, 0, 0},
            {0, 9, 8, 0, 0, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 0, 0, 0, 3},
            {4, 0, 0, 8, 0, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 0, 0, 0, 6},
            {0, 6, 0, 0, 0, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 0, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
        };
    }

    private int[][] createSolvedPuzzle() {
        return new int[][] {
            {5, 3, 4, 6, 7, 8, 9, 1, 2},
            {6, 7, 2, 1, 9, 5, 3, 4, 8},
            {1, 9, 8, 3, 4, 2, 5, 6, 7},
            {8, 5, 9, 7, 6, 1, 4, 2, 3},
            {4, 2, 6, 8, 5, 3, 7, 9, 1},
            {7, 1, 3, 9, 2, 4, 8, 5, 6},
            {9, 6, 1, 5, 3, 7, 2, 8, 4},
            {2, 8, 7, 4, 1, 9, 6, 3, 5},
            {3, 4, 5, 2, 8, 6, 1, 7, 9}
        };
    }

    @Test
    public void testSolveSolvablePuzzle() {
        int[][] puzzle = createSolvablePuzzle();
        boolean solved = solver.solve(puzzle);
        assertTrue(solved, "Solvable puzzle must be solved successfully");
        assertTrue(solver.isBoardValid(puzzle), "Solution must be valid");
    }

    @Test
    public void testAlreadySolvedPuzzle() {
        int[][] solved = createSolvedPuzzle();
        assertTrue(solver.isBoardValid(solved));
        assertEquals(1, solver.countSolutions(solved, 2), "Solved board should have exactly 1 solution");
        assertTrue(solver.solve(solved));
    }

    @Test
    public void testInvalidPuzzleRejected() {
        int[][] invalid = createSolvablePuzzle();
        // Insert duplicate in the same row
        invalid[0][2] = 5; // row 0 already has 5 at [0][0]
        assertFalse(solver.isBoardValid(invalid), "Board with duplicate in row must be invalid");
        assertFalse(solver.solve(invalid), "Solving invalid board must return false");
        assertEquals(0, solver.countSolutions(invalid, 2), "Invalid board must have 0 solutions");
    }

    @Test
    public void testUnsolvablePuzzle() {
        int[][] unsolvable = createSolvablePuzzle();
        // Force a contradiction that has no legal placements
        unsolvable[0][2] = 1;
        unsolvable[0][3] = 2;
        unsolvable[0][5] = 4;
        unsolvable[0][6] = 6;
        unsolvable[0][7] = 8;
        unsolvable[0][8] = 9;
        // At this point row 0 has {5,3,1,2,7,4,6,8,9} but let's make an impossible constraint
        unsolvable[1][1] = 7; // creates conflict in box or column
        if (solver.isBoardValid(unsolvable)) {
            assertFalse(solver.solve(unsolvable));
        } else {
            assertFalse(solver.solve(unsolvable));
        }
    }

    @Test
    public void testCountSolutionsWithMultipleSolutions() {
        // Very sparse board with multiple solutions
        int[][] sparseBoard = new int[9][9];
        sparseBoard[0][0] = 1;
        sparseBoard[1][1] = 2;
        sparseBoard[2][2] = 3;

        int solutions = solver.countSolutions(sparseBoard, 2);
        assertTrue(solutions >= 2, "Sparse board must have more than 1 solution");
    }

    @Test
    public void testCountSolutionsWithSingleSolution() {
        int[][] singleSolPuzzle = createSolvablePuzzle();
        int solutions = solver.countSolutions(singleSolPuzzle, 2);
        assertEquals(1, solutions, "Standard single-solution puzzle must yield count of 1");
    }
}
