package com.sudoku.service;

import com.sudoku.dto.CellPosition;
import com.sudoku.model.SudokuBoard;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SudokuValidationService {

    public record ValidationResult(boolean valid, String reason) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }
        public static ValidationResult error(String reason) {
            return new ValidationResult(false, reason);
        }
    }

    /**
     * Validates placing a value in (row, col) on the board according to standard Sudoku rules.
     * Value 0 (erasing a cell) is always valid as long as the cell is not fixed.
     */
    public ValidationResult validateMove(SudokuBoard board, int row, int col, int value) {
        if (value == 0) {
            return ValidationResult.ok();
        }

        if (value < 1 || value > 9) {
            return ValidationResult.error("Value must be between 1 and 9");
        }

        if (!board.isValidCoordinate(row, col)) {
            return ValidationResult.error("Invalid board coordinate (" + row + ", " + col + ")");
        }

        // 1. Check Row
        for (int c = 0; c < SudokuBoard.SIZE; c++) {
            if (c != col && board.getCell(row, c) == value) {
                return ValidationResult.error("Number already exists in row " + (row + 1));
            }
        }

        // 2. Check Column
        for (int r = 0; r < SudokuBoard.SIZE; r++) {
            if (r != row && board.getCell(r, col) == value) {
                return ValidationResult.error("Number already exists in column " + (col + 1));
            }
        }

        // 3. Check 3x3 Block
        int startRow = (row / SudokuBoard.BOX_SIZE) * SudokuBoard.BOX_SIZE;
        int startCol = (col / SudokuBoard.BOX_SIZE) * SudokuBoard.BOX_SIZE;

        for (int r = startRow; r < startRow + SudokuBoard.BOX_SIZE; r++) {
            for (int c = startCol; c < startCol + SudokuBoard.BOX_SIZE; c++) {
                if ((r != row || c != col) && board.getCell(r, c) == value) {
                    return ValidationResult.error("Number already exists in the 3x3 block");
                }
            }
        }

        return ValidationResult.ok();
    }

    /**
     * Validates whether the complete 9x9 board is completely solved and valid according to Sudoku rules.
     */
    public boolean isBoardSolved(SudokuBoard board) {
        if (!board.isFull()) {
            return false;
        }

        // Check each row
        for (int r = 0; r < SudokuBoard.SIZE; r++) {
            Set<Integer> seen = new HashSet<>();
            for (int c = 0; c < SudokuBoard.SIZE; c++) {
                int val = board.getCell(r, c);
                if (val < 1 || val > 9 || !seen.add(val)) {
                    return false;
                }
            }
        }

        // Check each column
        for (int c = 0; c < SudokuBoard.SIZE; c++) {
            Set<Integer> seen = new HashSet<>();
            for (int r = 0; r < SudokuBoard.SIZE; r++) {
                int val = board.getCell(r, c);
                if (val < 1 || val > 9 || !seen.add(val)) {
                    return false;
                }
            }
        }

        // Check each 3x3 box
        for (int boxRow = 0; boxRow < SudokuBoard.SIZE; boxRow += SudokuBoard.BOX_SIZE) {
            for (int boxCol = 0; boxCol < SudokuBoard.SIZE; boxCol += SudokuBoard.BOX_SIZE) {
                Set<Integer> seen = new HashSet<>();
                for (int r = 0; r < SudokuBoard.BOX_SIZE; r++) {
                    for (int c = 0; c < SudokuBoard.BOX_SIZE; c++) {
                        int val = board.getCell(boxRow + r, boxCol + c);
                        if (val < 1 || val > 9 || !seen.add(val)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Finds any cells in the board that currently conflict with other cells or with the solution.
     */
    public List<CellPosition> findIncorrectCells(SudokuBoard currentBoard, SudokuBoard solutionBoard) {
        List<CellPosition> incorrect = new ArrayList<>();

        if (solutionBoard != null && solutionBoard.isFull()) {
            for (int r = 0; r < SudokuBoard.SIZE; r++) {
                for (int c = 0; c < SudokuBoard.SIZE; c++) {
                    int val = currentBoard.getCell(r, c);
                    if (val != 0 && val != solutionBoard.getCell(r, c)) {
                        incorrect.add(new CellPosition(r, c));
                    }
                }
            }
            return incorrect;
        }

        // If no solution board provided, detect any rule violations on filled cells
        for (int r = 0; r < SudokuBoard.SIZE; r++) {
            for (int c = 0; c < SudokuBoard.SIZE; c++) {
                int val = currentBoard.getCell(r, c);
                if (val != 0) {
                    // Temporarily remove to check if it violates
                    currentBoard.setCell(r, c, 0);
                    ValidationResult res = validateMove(currentBoard, r, c, val);
                    currentBoard.setCell(r, c, val);
                    if (!res.valid()) {
                        incorrect.add(new CellPosition(r, c));
                    }
                }
            }
        }

        return incorrect;
    }
}
