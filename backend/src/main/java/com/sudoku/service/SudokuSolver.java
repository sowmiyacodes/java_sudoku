package com.sudoku.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class SudokuSolver {

    private final Random random = new Random();

    /**
     * Solves the Sudoku board. Returns true if a solution is found.
     * Randomized backtracking.
     */
    public boolean solve(int[][] board) {
        if (!isBoardValid(board)) {
            return false;
        }
        return solveRecursive(board);
    }

    private boolean solveRecursive(int[][] board) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
                    Collections.shuffle(nums, random);

                    for (int num : nums) {
                        if (isValidPlacement(board, r, c, num)) {
                            board[r][c] = num;
                            if (solveRecursive(board)) {
                                return true;
                            }
                            board[r][c] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Counts the number of solutions up to the given limit.
     * Useful for checking if a puzzle has exactly one solution (limit = 2).
     */
    public int countSolutions(int[][] board, int limit) {
        if (!isBoardValid(board)) {
            return 0;
        }
        return countSolutionsRecursive(board, limit, 0);
    }

    private int countSolutionsRecursive(int[][] board, int limit, int currentCount) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    for (int num = 1; num <= 9; num++) {
                        if (isValidPlacement(board, r, c, num)) {
                            board[r][c] = num;
                            currentCount = countSolutionsRecursive(board, limit, currentCount);
                            board[r][c] = 0;
                            
                            if (currentCount >= limit) {
                                return currentCount;
                            }
                        }
                    }
                    return currentCount;
                }
            }
        }
        // Found a solution
        return currentCount + 1;
    }

    /**
     * Validates whether the existing filled cells on the board do not violate Sudoku rules.
     */
    public boolean isBoardValid(int[][] board) {
        if (board == null || board.length != 9) return false;
        for (int r = 0; r < 9; r++) {
            if (board[r] == null || board[r].length != 9) return false;
            for (int c = 0; c < 9; c++) {
                int val = board[r][c];
                if (val < 0 || val > 9) return false;
                if (val != 0) {
                    // Check duplicate in same row
                    for (int i = 0; i < 9; i++) {
                        if (i != c && board[r][i] == val) return false;
                    }
                    // Check duplicate in same column
                    for (int i = 0; i < 9; i++) {
                        if (i != r && board[i][c] == val) return false;
                    }
                    // Check duplicate in 3x3 box
                    int startRow = (r / 3) * 3;
                    int startCol = (c / 3) * 3;
                    for (int br = 0; br < 3; br++) {
                        for (int bc = 0; bc < 3; bc++) {
                            int currR = startRow + br;
                            int currC = startCol + bc;
                            if ((currR != r || currC != c) && board[currR][currC] == val) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean isValidPlacement(int[][] board, int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            // Check row and column
            if (board[row][i] == num || board[i][col] == num) {
                return false;
            }
        }
        // Check 3x3 box
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[startRow + r][startCol + c] == num) {
                    return false;
                }
            }
        }
        return true;
    }
}
