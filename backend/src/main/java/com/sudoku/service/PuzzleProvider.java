package com.sudoku.service;

import com.sudoku.dto.DifficultyAnalysis;
import com.sudoku.repository.PuzzleRepository;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class PuzzleProvider {

    public record Puzzle(String id, String difficulty, int[][] initialBoard, int[][] solutionBoard, DifficultyAnalysis metadata) {}

    private final Random random = new Random();
    private final SudokuSolver solver;
    private final SudokuDifficultyService difficultyService;
    private final PuzzleRepository puzzleRepository;

    @Autowired
    public PuzzleProvider(SudokuSolver solver, SudokuDifficultyService difficultyService, PuzzleRepository puzzleRepository) {
        this.solver = solver;
        this.difficultyService = difficultyService;
        this.puzzleRepository = puzzleRepository;
    }

    public PuzzleProvider(SudokuSolver solver, SudokuDifficultyService difficultyService) {
        this.solver = solver;
        this.difficultyService = difficultyService;
        this.puzzleRepository = null;
    }

    public Puzzle getPuzzle(String puzzleId) {
        if (puzzleRepository != null) {
            com.sudoku.model.Puzzle stored = puzzleRepository.findByPuzzleIdAndActiveTrue(puzzleId).orElse(null);
            if (stored != null) return toPuzzle(stored);
        }
        return generateDynamicPuzzle("Medium");
    }

    public Puzzle getPuzzleByDifficulty(String difficulty) {
        String diff = (difficulty != null && !difficulty.trim().isEmpty()) ? difficulty : "Medium";
        if (puzzleRepository != null) {
            List<com.sudoku.model.Puzzle> stored = puzzleRepository.findByDifficultyIgnoreCaseAndActiveTrue(diff);
            if (!stored.isEmpty()) return toPuzzle(stored.get(random.nextInt(stored.size())));
        }
        return generateDynamicPuzzle(diff);
    }

    private Puzzle toPuzzle(com.sudoku.model.Puzzle stored) {
        int[][] initialBoard = decode(stored.getPuzzle());
        int[][] solutionBoard = decode(stored.getSolution());
        int emptyCells = (int) stored.getPuzzle().chars().filter(c -> c == '.' || c == '0').count();
        DifficultyAnalysis metadata = new DifficultyAnalysis(
                capitalize(stored.getDifficulty()), stored.getDifficultyScore() == null ? 0 : stored.getDifficultyScore(),
                emptyCells, 0, 0, List.of(), true);
        return new Puzzle(stored.getPuzzleId(), capitalize(stored.getDifficulty()), initialBoard, solutionBoard, metadata);
    }

    private int[][] decode(String puzzle) {
        int[][] grid = new int[9][9];
        for (int i = 0; i < 81; i++) {
            char cell = puzzle.charAt(i);
            grid[i / 9][i % 9] = cell == '.' || cell == '0' ? 0 : cell - '0';
        }
        return grid;
    }

    public Puzzle getDefaultPuzzle() {
        return generateDynamicPuzzle("Medium");
    }

    public Puzzle generateDynamicPuzzle(String targetDifficulty) {
        String target = capitalize(targetDifficulty);
        int maxAttempts = 8;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int[][] solutionBoard = new int[9][9];

            // 1. Fill diagonal 3x3 boxes randomly
            fillDiagonalBoxes(solutionBoard);

            // 2. Solve the remaining board using randomized backtracking
            solver.solve(solutionBoard);

            // 3. Clone solution board for initial board
            int[][] initialBoard = new int[9][9];
            for (int r = 0; r < 9; r++) {
                System.arraycopy(solutionBoard[r], 0, initialBoard[r], 0, 9);
            }

            // 4. Remove cells according to chosen difficulty while preserving unique solution
            int targetEmptyCells = getTargetEmptyCells(target);
            removeCellsGuaranteedUnique(initialBoard, targetEmptyCells);

            DifficultyAnalysis metadata = difficultyService.analyzeDifficulty(initialBoard);

            if (metadata.getDifficulty().equalsIgnoreCase(target)) {
                String puzzleId = "dyn-" + metadata.getDifficulty().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8);
                return new Puzzle(puzzleId, metadata.getDifficulty(), initialBoard, solutionBoard, metadata);
            }
        }

        throw new IllegalStateException("Failed to dynamically generate puzzle matching requested difficulty: " + target);
    }

    private String capitalize(String text) {
        if (text == null || text.trim().isEmpty()) return "Medium";
        String trimmed = text.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    private void fillDiagonalBoxes(int[][] board) {
        for (int i = 0; i < 9; i += 3) {
            fillBox(board, i, i);
        }
    }

    private void fillBox(int[][] board, int row, int col) {
        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(nums, random);
        int idx = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board[row + r][col + c] = nums.get(idx++);
            }
        }
    }

    private int getTargetEmptyCells(String difficulty) {
        String diff = (difficulty != null) ? difficulty.toLowerCase() : "medium";
        return switch (diff) {
            case "easy" -> 33 + random.nextInt(4);      // ~33-36 empty cells
            case "hard" -> 50 + random.nextInt(2);      // ~50-51 empty cells
            case "expert" -> 55 + random.nextInt(3);    // ~55-57 empty cells
            default -> 43 + random.nextInt(3);          // Medium: ~43-45 empty cells
        };
    }

    private void removeCellsGuaranteedUnique(int[][] board, int targetEmpty) {
        List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < 81; i++) cells.add(i);
        Collections.shuffle(cells, random);

        int emptyCount = 0;
        
        for (int cellId : cells) {
            if (emptyCount >= targetEmpty) {
                break;
            }
            
            int r = cellId / 9;
            int c = cellId % 9;
            
            int backup = board[r][c];
            if (backup != 0) {
                board[r][c] = 0;
                
                // Copy board to not mutate the main one during counting
                int[][] tempBoard = new int[9][9];
                for (int i = 0; i < 9; i++) {
                    System.arraycopy(board[i], 0, tempBoard[i], 0, 9);
                }
                
                int solutions = solver.countSolutions(tempBoard, 2);
                
                if (solutions == 1) {
                    emptyCount++;
                } else {
                    board[r][c] = backup; // Restore
                }
            }
        }
    }
}
