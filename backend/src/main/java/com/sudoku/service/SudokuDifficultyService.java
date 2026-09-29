package com.sudoku.service;

import com.sudoku.dto.DifficultyAnalysis;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SudokuDifficultyService {

    private final SudokuCandidateService candidateService;

    public SudokuDifficultyService(SudokuCandidateService candidateService) {
        this.candidateService = candidateService;
    }

    public DifficultyAnalysis analyzeDifficulty(int[][] board) {
        if (board == null || board.length != 9) {
            return new DifficultyAnalysis("Medium", 100, 45, 120, 45, List.of("Naked Single"), true);
        }

        // 1. Initial board metrics
        int emptyCells = 0;
        int totalCandidates = 0;
        List<Integer>[][] initialCandidates = candidateService.getAllCandidates(board);

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    emptyCells++;
                    totalCandidates += initialCandidates[r][c].size();
                }
            }
        }

        double candidateDensity = emptyCells > 0 ? (double) totalCandidates / emptyCells : 0;

        // 2. Clone board for simulation
        int[][] workingBoard = new int[9][9];
        for (int r = 0; r < 9; r++) {
            System.arraycopy(board[r], 0, workingBoard[r], 0, 9);
        }

        Set<String> techniques = new LinkedHashSet<>();
        int nakedSingles = 0;
        int hiddenSingles = 0;
        int logicalSteps = 0;
        boolean progress = true;

        // 3. Step-by-step logical reduction
        while (progress) {
            progress = false;
            List<Integer>[][] currentCandidates = candidateService.getAllCandidates(workingBoard);

            // A. Check for Naked Singles
            int nsRow = -1, nsCol = -1, nsVal = -1;
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (workingBoard[r][c] == 0 && currentCandidates[r][c].size() == 1) {
                        nsRow = r;
                        nsCol = c;
                        nsVal = currentCandidates[r][c].get(0);
                        break;
                    }
                }
                if (nsRow != -1) break;
            }

            if (nsRow != -1) {
                workingBoard[nsRow][nsCol] = nsVal;
                nakedSingles++;
                logicalSteps++;
                techniques.add("Naked Single");
                progress = true;
                continue;
            }

            // B. Check for Hidden Singles (Row)
            boolean foundHidden = false;
            for (int r = 0; r < 9 && !foundHidden; r++) {
                for (int val = 1; val <= 9 && !foundHidden; val++) {
                    int count = 0;
                    int targetCol = -1;
                    for (int c = 0; c < 9; c++) {
                        if (workingBoard[r][c] == 0 && currentCandidates[r][c].contains(val)) {
                            count++;
                            targetCol = c;
                        }
                    }
                    if (count == 1) {
                        workingBoard[r][targetCol] = val;
                        hiddenSingles++;
                        logicalSteps++;
                        techniques.add("Hidden Single");
                        progress = true;
                        foundHidden = true;
                    }
                }
            }
            if (foundHidden) continue;

            // C. Check for Hidden Singles (Column)
            for (int c = 0; c < 9 && !foundHidden; c++) {
                for (int val = 1; val <= 9 && !foundHidden; val++) {
                    int count = 0;
                    int targetRow = -1;
                    for (int r = 0; r < 9; r++) {
                        if (workingBoard[r][c] == 0 && currentCandidates[r][c].contains(val)) {
                            count++;
                            targetRow = r;
                        }
                    }
                    if (count == 1) {
                        workingBoard[targetRow][c] = val;
                        hiddenSingles++;
                        logicalSteps++;
                        techniques.add("Hidden Single");
                        progress = true;
                        foundHidden = true;
                    }
                }
            }
            if (foundHidden) continue;

            // D. Check for Hidden Singles (3x3 Box)
            for (int box = 0; box < 9 && !foundHidden; box++) {
                int br = (box / 3) * 3;
                int bc = (box % 3) * 3;
                for (int val = 1; val <= 9 && !foundHidden; val++) {
                    int count = 0;
                    int targetR = -1;
                    int targetC = -1;
                    for (int r = 0; r < 3; r++) {
                        for (int c = 0; c < 3; c++) {
                            int row = br + r;
                            int col = bc + c;
                            if (workingBoard[row][col] == 0 && currentCandidates[row][col].contains(val)) {
                                count++;
                                targetR = row;
                                targetC = col;
                            }
                        }
                    }
                    if (count == 1) {
                        workingBoard[targetR][targetC] = val;
                        hiddenSingles++;
                        logicalSteps++;
                        techniques.add("Hidden Single");
                        progress = true;
                        foundHidden = true;
                    }
                }
            }
        }

        // 4. If board is not full, measure branching / backtracking nodes required
        int remainingEmpty = 0;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (workingBoard[r][c] == 0) remainingEmpty++;
            }
        }

        int branchingNodes = 0;
        if (remainingEmpty > 0) {
            techniques.add("Candidate Branching");
            branchingNodes = countBacktrackNodes(workingBoard, 0, 50);
        }

        int totalSolvingSteps = logicalSteps + branchingNodes;

        // 5. Algorithmic difficulty scoring formula
        int score = (int) Math.round(
                (emptyCells * 1.4) +
                (candidateDensity * 8.0) +
                (nakedSingles * 0.5) +
                (hiddenSingles * 2.0) +
                (branchingNodes * 2.5)
        );

        // 6. Dynamic classification into EASY, MEDIUM, HARD, EXPERT
        String difficulty;
        if (emptyCells <= 38 || score < 85) {
            difficulty = "Easy";
        } else if (emptyCells <= 47 && score < 135) {
            difficulty = "Medium";
        } else if (emptyCells <= 53 && score < 185) {
            difficulty = "Hard";
        } else {
            difficulty = "Expert";
        }

        if (techniques.isEmpty()) {
            techniques.add("Direct Deduction");
        }

        return new DifficultyAnalysis(
                difficulty,
                score,
                emptyCells,
                totalCandidates,
                totalSolvingSteps,
                new ArrayList<>(techniques),
                true
        );
    }

    private int countBacktrackNodes(int[][] board, int currentNodes, int maxLimit) {
        if (currentNodes >= maxLimit) {
            return currentNodes;
        }

        // Find empty cell with fewest candidates
        int bestR = -1;
        int bestC = -1;
        List<Integer> bestCandidates = null;

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    List<Integer> candidates = candidateService.getCandidates(board, r, c);
                    if (candidates.isEmpty()) {
                        return currentNodes + 1; // Dead end branch
                    }
                    if (bestCandidates == null || candidates.size() < bestCandidates.size()) {
                        bestR = r;
                        bestC = c;
                        bestCandidates = candidates;
                        if (bestCandidates.size() == 2) break; // Good enough heuristic
                    }
                }
            }
            if (bestCandidates != null && bestCandidates.size() == 2) break;
        }

        if (bestR == -1) {
            return currentNodes; // Board filled
        }

        int nodes = currentNodes + 1;
        for (int val : bestCandidates) {
            board[bestR][bestC] = val;
            nodes = countBacktrackNodes(board, nodes, maxLimit);
            board[bestR][bestC] = 0;
            if (nodes >= maxLimit) {
                return nodes;
            }
        }
        return nodes;
    }
}
