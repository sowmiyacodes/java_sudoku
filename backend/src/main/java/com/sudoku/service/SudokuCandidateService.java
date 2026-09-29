package com.sudoku.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SudokuCandidateService {

    /**
     * Get candidates for a single cell on the board.
     * Returns empty list if cell is already filled.
     */
    public List<Integer> getCandidates(int[][] board, int row, int col) {
        if (board[row][col] != 0) {
            return new ArrayList<>();
        }

        boolean[] used = new boolean[10];
        
        // Check row
        for (int c = 0; c < 9; c++) {
            if (board[row][c] != 0) {
                used[board[row][c]] = true;
            }
        }
        
        // Check column
        for (int r = 0; r < 9; r++) {
            if (board[r][col] != 0) {
                used[board[r][col]] = true;
            }
        }
        
        // Check box
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[startRow + r][startCol + c] != 0) {
                    used[board[startRow + r][startCol + c]] = true;
                }
            }
        }
        
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            if (!used[i]) {
                candidates.add(i);
            }
        }
        return candidates;
    }

    /**
     * Gets candidates for all empty cells.
     * Returns a 9x9 grid of candidate lists.
     */
    public List<Integer>[][] getAllCandidates(int[][] board) {
        @SuppressWarnings("unchecked")
        List<Integer>[][] allCandidates = new List[9][9];
        
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    allCandidates[r][c] = getCandidates(board, r, c);
                } else {
                    allCandidates[r][c] = new ArrayList<>();
                }
            }
        }
        return allCandidates;
    }
}
