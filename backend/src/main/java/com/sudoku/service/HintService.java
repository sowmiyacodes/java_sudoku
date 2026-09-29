package com.sudoku.service;

import com.sudoku.dto.HintResponse;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HintService {

    private final SudokuCandidateService candidateService;

    public HintService(SudokuCandidateService candidateService) {
        this.candidateService = candidateService;
    }

    public HintResponse getHint(int[][] board) {
        return getHint(board, 3);
    }

    public HintResponse getHint(int[][] board, int requestedLevel) {
        int level = Math.max(1, Math.min(3, requestedLevel));
        List<Integer>[][] candidates = candidateService.getAllCandidates(board);

        // 1. Check Naked Single
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0 && candidates[r][c].size() == 1) {
                    int val = candidates[r][c].get(0);
                    return withLevel(HintResponse.available(r, c, val, "NAKED_SINGLE",
                            "This cell has only one possible value.", candidates[r][c]), level);
                }
            }
        }

        // 2. Check Hidden Single
        // Check rows
        for (int r = 0; r < 9; r++) {
            for (int val = 1; val <= 9; val++) {
                int possibleCols = 0;
                int lastCol = -1;
                for (int c = 0; c < 9; c++) {
                    if (board[r][c] == 0 && candidates[r][c].contains(val)) {
                        possibleCols++;
                        lastCol = c;
                    }
                }
                if (possibleCols == 1) {
                    return withLevel(HintResponse.available(r, lastCol, val, "HIDDEN_SINGLE",
                            "In this row, " + val + " can only be placed in this cell.", candidates[r][lastCol]), level);
                }
            }
        }

        // Check cols
        for (int c = 0; c < 9; c++) {
            for (int val = 1; val <= 9; val++) {
                int possibleRows = 0;
                int lastRow = -1;
                for (int r = 0; r < 9; r++) {
                    if (board[r][c] == 0 && candidates[r][c].contains(val)) {
                        possibleRows++;
                        lastRow = r;
                    }
                }
                if (possibleRows == 1) {
                    return withLevel(HintResponse.available(lastRow, c, val, "HIDDEN_SINGLE",
                            "In this column, " + val + " can only be placed in this cell.", candidates[lastRow][c]), level);
                }
            }
        }

        // Check boxes
        for (int box = 0; box < 9; box++) {
            int startRow = (box / 3) * 3;
            int startCol = (box % 3) * 3;
            for (int val = 1; val <= 9; val++) {
                int possibleCells = 0;
                int lastR = -1;
                int lastC = -1;
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        int row = startRow + r;
                        int col = startCol + c;
                        if (board[row][col] == 0 && candidates[row][col].contains(val)) {
                            possibleCells++;
                            lastR = row;
                            lastC = col;
                        }
                    }
                }
                if (possibleCells == 1) {
                    return withLevel(HintResponse.available(lastR, lastC, val, "HIDDEN_SINGLE",
                            "In this 3x3 box, " + val + " can only be placed in this cell.", candidates[lastR][lastC]), level);
                }
            }
        }

        HintResponse advancedHint = findNakedPair(board, candidates, level);
        if (advancedHint != null) {
            return advancedHint;
        }

        advancedHint = findHiddenPair(board, candidates, level);
        if (advancedHint != null) {
            return advancedHint;
        }

        advancedHint = findNakedTriple(board, candidates, level);
        if (advancedHint != null) {
            return advancedHint;
        }

        advancedHint = findPointingPair(board, candidates, level);
        if (advancedHint != null) {
            return advancedHint;
        }

        advancedHint = findXWing(board, candidates, level);
        if (advancedHint != null) {
            return advancedHint;
        }

        return HintResponse.notAvailable("No logical deduction is currently available.");
    }

    private HintResponse findNakedPair(int[][] board, List<Integer>[][] candidates, int level) {
        for (int unit = 0; unit < 27; unit++) {
            List<int[]> cells = getUnitCells(unit);
            for (int first = 0; first < cells.size(); first++) {
                int[] firstCell = cells.get(first);
                if (board[firstCell[0]][firstCell[1]] != 0 || candidates[firstCell[0]][firstCell[1]].size() != 2) {
                    continue;
                }
                for (int second = first + 1; second < cells.size(); second++) {
                    int[] secondCell = cells.get(second);
                    if (!candidates[firstCell[0]][firstCell[1]].equals(candidates[secondCell[0]][secondCell[1]])) {
                        continue;
                    }
                    List<Integer> pair = candidates[firstCell[0]][firstCell[1]];
                    for (int[] target : cells) {
                        if ((target[0] == firstCell[0] && target[1] == firstCell[1])
                                || (target[0] == secondCell[0] && target[1] == secondCell[1])
                                || board[target[0]][target[1]] != 0) {
                            continue;
                        }
                        List<Integer> targetCandidates = candidates[target[0]][target[1]];
                        if (targetCandidates.contains(pair.get(0)) || targetCandidates.contains(pair.get(1))) {
                            String unitName = unitName(unit);
                            String explanation = "In " + unitName + ", " + pair
                                    + " is locked into R" + (firstCell[0] + 1) + "C" + (firstCell[1] + 1)
                                    + " and R" + (secondCell[0] + 1) + "C" + (secondCell[1] + 1)
                                    + ". Remove those candidates from this cell.";
                            return withLevel(HintResponse.available(target[0], target[1], 0,
                                    "NAKED_PAIR", explanation, targetCandidates), level);
                        }
                    }
                }
            }
        }
        return null;
    }

    private HintResponse findHiddenPair(int[][] board, List<Integer>[][] candidates, int level) {
        for (int unit = 0; unit < 27; unit++) {
            List<int[]> cells = getUnitCells(unit);
            for (int firstValue = 1; firstValue < 9; firstValue++) {
                List<int[]> firstLocations = locationsForValue(board, candidates, cells, firstValue);
                if (firstLocations.size() != 2) continue;
                for (int secondValue = firstValue + 1; secondValue <= 9; secondValue++) {
                    List<int[]> secondLocations = locationsForValue(board, candidates, cells, secondValue);
                    if (secondLocations.size() != 2 || !sameLocations(firstLocations, secondLocations)) continue;
                    for (int[] target : firstLocations) {
                        List<Integer> targetCandidates = candidates[target[0]][target[1]];
                        if (targetCandidates.size() > 2) {
                            List<Integer> pair = List.of(firstValue, secondValue);
                            String explanation = "In " + unitName(unit) + ", " + pair
                                    + " can only appear in R" + (firstLocations.get(0)[0] + 1)
                                    + "C" + (firstLocations.get(0)[1] + 1) + " and R"
                                    + (firstLocations.get(1)[0] + 1) + "C" + (firstLocations.get(1)[1] + 1)
                                    + ". Keep only those candidates in this cell.";
                            return withLevel(HintResponse.available(target[0], target[1], 0,
                                    "HIDDEN_PAIR", explanation, targetCandidates), level);
                        }
                    }
                }
            }
        }
        return null;
    }

    private HintResponse findNakedTriple(int[][] board, List<Integer>[][] candidates, int level) {
        for (int unit = 0; unit < 27; unit++) {
            List<int[]> cells = getUnitCells(unit);
            for (int first = 0; first < cells.size() - 2; first++) {
                for (int second = first + 1; second < cells.size() - 1; second++) {
                    for (int third = second + 1; third < cells.size(); third++) {
                        int[] firstCell = cells.get(first);
                        int[] secondCell = cells.get(second);
                        int[] thirdCell = cells.get(third);
                        if (!isNakedTripleCell(board, candidates, firstCell)
                                || !isNakedTripleCell(board, candidates, secondCell)
                                || !isNakedTripleCell(board, candidates, thirdCell)) {
                            continue;
                        }

                        Set<Integer> triple = new HashSet<>();
                        triple.addAll(candidates[firstCell[0]][firstCell[1]]);
                        triple.addAll(candidates[secondCell[0]][secondCell[1]]);
                        triple.addAll(candidates[thirdCell[0]][thirdCell[1]]);
                        if (triple.size() != 3) continue;

                        for (int[] target : cells) {
                            if (sameCell(target, firstCell) || sameCell(target, secondCell)
                                    || sameCell(target, thirdCell) || board[target[0]][target[1]] != 0) {
                                continue;
                            }
                            List<Integer> targetCandidates = candidates[target[0]][target[1]];
                            if (targetCandidates.stream().anyMatch(triple::contains)) {
                                String explanation = "In " + unitName(unit) + ", three cells form a Naked Triple "
                                        + triple + ". Remove those candidates from this cell.";
                                return withLevel(HintResponse.available(target[0], target[1], 0,
                                        "NAKED_TRIPLE", explanation, targetCandidates), level);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<int[]> locationsForValue(int[][] board, List<Integer>[][] candidates,
                                          List<int[]> cells, int value) {
        List<int[]> locations = new java.util.ArrayList<>();
        for (int[] cell : cells) {
            if (board[cell[0]][cell[1]] == 0 && candidates[cell[0]][cell[1]].contains(value)) {
                locations.add(cell);
            }
        }
        return locations;
    }

    private boolean sameLocations(List<int[]> first, List<int[]> second) {
        return first.stream().allMatch(cell -> second.stream().anyMatch(other -> sameCell(cell, other)));
    }

    private boolean isNakedTripleCell(int[][] board, List<Integer>[][] candidates, int[] cell) {
        return board[cell[0]][cell[1]] == 0
                && candidates[cell[0]][cell[1]].size() >= 2
                && candidates[cell[0]][cell[1]].size() <= 3;
    }

    private boolean sameCell(int[] first, int[] second) {
        return first[0] == second[0] && first[1] == second[1];
    }

    private HintResponse findPointingPair(int[][] board, List<Integer>[][] candidates, int level) {
        for (int box = 0; box < 9; box++) {
            int startRow = (box / 3) * 3;
            int startCol = (box % 3) * 3;
            for (int value = 1; value <= 9; value++) {
                List<int[]> locations = new java.util.ArrayList<>();
                for (int row = startRow; row < startRow + 3; row++) {
                    for (int col = startCol; col < startCol + 3; col++) {
                        if (board[row][col] == 0 && candidates[row][col].contains(value)) {
                            locations.add(new int[] { row, col });
                        }
                    }
                }
                if (locations.size() < 2) {
                    continue;
                }

                boolean sameRow = locations.stream().map(cell -> cell[0]).distinct().count() == 1;
                boolean sameColumn = locations.stream().map(cell -> cell[1]).distinct().count() == 1;
                if (sameRow) {
                    int row = locations.get(0)[0];
                    for (int col = 0; col < 9; col++) {
                        if (col >= startCol && col < startCol + 3 || board[row][col] != 0) {
                            continue;
                        }
                        if (candidates[row][col].contains(value)) {
                            String explanation = "In box " + (box + 1) + ", " + value
                                    + " can only appear on row " + (row + 1)
                                    + ". Remove it from the rest of that row.";
                            return withLevel(HintResponse.available(row, col, 0,
                                    "POINTING_PAIR", explanation, candidates[row][col]), level);
                        }
                    }
                }
                if (sameColumn) {
                    int col = locations.get(0)[1];
                    for (int row = 0; row < 9; row++) {
                        if (row >= startRow && row < startRow + 3 || board[row][col] != 0) {
                            continue;
                        }
                        if (candidates[row][col].contains(value)) {
                            String explanation = "In box " + (box + 1) + ", " + value
                                    + " can only appear on column " + (col + 1)
                                    + ". Remove it from the rest of that column.";
                            return withLevel(HintResponse.available(row, col, 0,
                                    "POINTING_PAIR", explanation, candidates[row][col]), level);
                        }
                    }
                }
            }
        }
        return null;
    }

    private HintResponse findXWing(int[][] board, List<Integer>[][] candidates, int level) {
        for (int value = 1; value <= 9; value++) {
            for (int firstRow = 0; firstRow < 8; firstRow++) {
                List<Integer> firstColumns = candidateColumns(board, candidates, firstRow, value);
                if (firstColumns.size() != 2) continue;
                for (int secondRow = firstRow + 1; secondRow < 9; secondRow++) {
                    List<Integer> secondColumns = candidateColumns(board, candidates, secondRow, value);
                    if (!firstColumns.equals(secondColumns)) continue;
                    for (int row = 0; row < 9; row++) {
                        if (row == firstRow || row == secondRow) continue;
                        for (int column : firstColumns) {
                            if (board[row][column] == 0 && candidates[row][column].contains(value)) {
                                String explanation = value + " forms an X-Wing on rows "
                                        + (firstRow + 1) + " and " + (secondRow + 1)
                                        + " across columns " + (firstColumns.get(0) + 1)
                                        + " and " + (firstColumns.get(1) + 1)
                                        + ". Remove it from this cell.";
                                return withLevel(HintResponse.available(row, column, 0,
                                        "X_WING", explanation, candidates[row][column]), level);
                            }
                        }
                    }
                }
            }

            for (int firstColumn = 0; firstColumn < 8; firstColumn++) {
                List<Integer> firstRows = candidateRows(board, candidates, firstColumn, value);
                if (firstRows.size() != 2) continue;
                for (int secondColumn = firstColumn + 1; secondColumn < 9; secondColumn++) {
                    List<Integer> secondRows = candidateRows(board, candidates, secondColumn, value);
                    if (!firstRows.equals(secondRows)) continue;
                    for (int column = 0; column < 9; column++) {
                        if (column == firstColumn || column == secondColumn) continue;
                        for (int row : firstRows) {
                            if (board[row][column] == 0 && candidates[row][column].contains(value)) {
                                String explanation = value + " forms an X-Wing on columns "
                                        + (firstColumn + 1) + " and " + (secondColumn + 1)
                                        + " across rows " + (firstRows.get(0) + 1)
                                        + " and " + (firstRows.get(1) + 1)
                                        + ". Remove it from this cell.";
                                return withLevel(HintResponse.available(row, column, 0,
                                        "X_WING", explanation, candidates[row][column]), level);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<Integer> candidateColumns(int[][] board, List<Integer>[][] candidates, int row, int value) {
        List<Integer> columns = new java.util.ArrayList<>();
        for (int column = 0; column < 9; column++) {
            if (board[row][column] == 0 && candidates[row][column].contains(value)) columns.add(column);
        }
        return columns;
    }

    private List<Integer> candidateRows(int[][] board, List<Integer>[][] candidates, int column, int value) {
        List<Integer> rows = new java.util.ArrayList<>();
        for (int row = 0; row < 9; row++) {
            if (board[row][column] == 0 && candidates[row][column].contains(value)) rows.add(row);
        }
        return rows;
    }

    private List<int[]> getUnitCells(int unit) {
        List<int[]> cells = new java.util.ArrayList<>();
        if (unit < 9) {
            for (int col = 0; col < 9; col++) cells.add(new int[] { unit, col });
        } else if (unit < 18) {
            for (int row = 0; row < 9; row++) cells.add(new int[] { row, unit - 9 });
        } else {
            int box = unit - 18;
            int startRow = (box / 3) * 3;
            int startCol = (box % 3) * 3;
            for (int row = startRow; row < startRow + 3; row++) {
                for (int col = startCol; col < startCol + 3; col++) cells.add(new int[] { row, col });
            }
        }
        return cells;
    }

    private String unitName(int unit) {
        if (unit < 9) return "row " + (unit + 1);
        if (unit < 18) return "column " + (unit - 8);
        return "box " + (unit - 17);
    }

    private HintResponse withLevel(HintResponse hint, int level) {
        hint.setLevel(level);
        String cell = "R" + (hint.getRow() + 1) + "C" + (hint.getColumn() + 1);
        if (level == 1) {
            hint.setHintText("Look at " + cell + ".");
        } else if (level == 2) {
            hint.setHintText(cell + " has candidates " + hint.getCandidates() + ". " + hint.getExplanation());
        } else if (hint.getValue() > 0) {
            hint.setHintText(cell + " = " + hint.getValue() + ". " + hint.getExplanation());
        } else {
            hint.setHintText(cell + ". " + hint.getExplanation());
        }
        return hint;
    }
}
