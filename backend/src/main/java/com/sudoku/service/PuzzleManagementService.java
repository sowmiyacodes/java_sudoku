package com.sudoku.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudoku.dto.PuzzleManagementRequest;
import com.sudoku.dto.PuzzleRecordResponse;
import com.sudoku.model.Puzzle;
import com.sudoku.repository.PuzzleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional
public class PuzzleManagementService {

    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD", "EXPERT");

    private final PuzzleRepository puzzleRepository;
    private final SudokuSolver solver;
    private final MLPredictionService mlPredictionService;
    private final ObjectMapper objectMapper;

    public PuzzleManagementService(PuzzleRepository puzzleRepository, SudokuSolver solver,
                                   MLPredictionService mlPredictionService, ObjectMapper objectMapper) {
        this.puzzleRepository = puzzleRepository;
        this.solver = solver;
        this.mlPredictionService = mlPredictionService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PuzzleRecordResponse> list() {
        return puzzleRepository.findAll().stream().map(PuzzleRecordResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PuzzleRecordResponse get(String puzzleId) {
        return PuzzleRecordResponse.from(find(puzzleId));
    }

    public PuzzleRecordResponse create(PuzzleManagementRequest request) {
        int[][] grid = parseAndValidate(request.puzzle());
        int[][] solution = copy(grid);
        if (solver.countSolutions(copy(grid), 2) != 1 || !solver.solve(solution)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Puzzle must have exactly one valid solution.");
        }
        Map<String, Object> prediction = mlPredictionService.predictDifficulty(encode(grid));
        String predictedDifficulty = String.valueOf(prediction.getOrDefault("difficulty", "MEDIUM")).toUpperCase(Locale.ROOT);
        String difficulty = normalizeDifficulty(request.difficulty(), predictedDifficulty);

        Puzzle puzzle = new Puzzle();
        puzzle.setPuzzleId("puz-" + UUID.randomUUID().toString().substring(0, 12));
        puzzle.setPuzzle(encode(grid));
        puzzle.setSolution(encode(solution));
        puzzle.setDifficulty(difficulty);
        puzzle.setSource(cleanSource(request.source()));
        puzzle.setRating(request.rating());
        puzzle.setActive(request.active() == null || request.active());
        applyPrediction(puzzle, prediction, predictedDifficulty);
        return PuzzleRecordResponse.from(puzzleRepository.save(puzzle));
    }

    public PuzzleRecordResponse update(String puzzleId, PuzzleManagementRequest request) {
        Puzzle puzzle = find(puzzleId);
        int[][] grid = parseAndValidate(request.puzzle());
        int[][] solution = copy(grid);
        if (solver.countSolutions(copy(grid), 2) != 1 || !solver.solve(solution)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Puzzle must have exactly one valid solution.");
        }
        Map<String, Object> prediction = mlPredictionService.predictDifficulty(encode(grid));
        String predictedDifficulty = String.valueOf(prediction.getOrDefault("difficulty", "MEDIUM")).toUpperCase(Locale.ROOT);
        puzzle.setPuzzle(encode(grid));
        puzzle.setSolution(encode(solution));
        puzzle.setDifficulty(normalizeDifficulty(request.difficulty(), predictedDifficulty));
        puzzle.setSource(cleanSource(request.source()));
        puzzle.setRating(request.rating());
        if (request.active() != null) puzzle.setActive(request.active());
        applyPrediction(puzzle, prediction, predictedDifficulty);
        return PuzzleRecordResponse.from(puzzleRepository.save(puzzle));
    }

    public void delete(String puzzleId) {
        puzzleRepository.delete(find(puzzleId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analytics() {
        List<Puzzle> puzzles = puzzleRepository.findAll();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String difficulty : List.of("EASY", "MEDIUM", "HARD", "EXPERT")) {
            counts.put(difficulty, puzzles.stream().filter(Puzzle::isActive)
                    .filter(p -> difficulty.equalsIgnoreCase(p.getDifficulty())).count());
        }
        double confidence = puzzles.stream().filter(Puzzle::isActive).map(Puzzle::getModelConfidence)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0.0);
        long agreements = puzzles.stream().filter(Puzzle::isActive)
                .filter(p -> p.getPredictedDifficulty() != null && p.getDifficulty() != null
                        && p.getPredictedDifficulty().equalsIgnoreCase(p.getDifficulty())).count();
        long evaluated = puzzles.stream().filter(Puzzle::isActive).filter(p -> p.getPredictedDifficulty() != null).count();
        return Map.of("total", puzzles.size(), "active", puzzles.stream().filter(Puzzle::isActive).count(),
                "difficultyCounts", counts, "averageConfidence", confidence,
            "predictionAgreement", evaluated == 0 ? 0.0 : (double) agreements / evaluated,
            "featureImportances", mlPredictionService.fetchDifficultyFeatureImportance()
                .getOrDefault("feature_importances", Map.of()));
    }

    private Puzzle find(String puzzleId) {
        return puzzleRepository.findByPuzzleId(puzzleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Puzzle not found."));
    }

    private int[][] parseAndValidate(String value) {
        if (value == null || !value.matches("[0-9.]{81}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Puzzle must contain exactly 81 digits, dots, or zeroes.");
        }
        int[][] grid = new int[9][9];
        for (int i = 0; i < value.length(); i++) {
            char cell = value.charAt(i);
            grid[i / 9][i % 9] = cell == '.' || cell == '0' ? 0 : cell - '0';
        }
        if (!solver.isBoardValid(grid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Puzzle contains conflicting givens.");
        }
        return grid;
    }

    private String normalizeDifficulty(String requested, String predicted) {
        String value = requested == null || requested.isBlank() ? predicted : requested.trim().toUpperCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Difficulty must be Easy, Medium, Hard, or Expert.");
        }
        return value;
    }

    private String cleanSource(String source) {
        return source == null || source.isBlank() ? "MANUAL" : source.trim().substring(0, Math.min(100, source.trim().length()));
    }

    private void applyPrediction(Puzzle puzzle, Map<String, Object> prediction, String predictedDifficulty) {
        puzzle.setPredictedDifficulty(predictedDifficulty);
        Object confidence = prediction.get("confidence");
        puzzle.setModelConfidence(confidence instanceof Number n ? n.doubleValue() : null);
        Object features = prediction.get("extracted_features");
        Object factors = prediction.get("top_factors");
        puzzle.setFeaturesJson(toJson(features));
        puzzle.setTopFactorsJson(toJson(factors));
        Object clueCount = features instanceof Map<?, ?> map ? map.get("clue_count") : null;
        puzzle.setDifficultyScore(clueCount instanceof Number n ? 81 - n.intValue() : null);
    }

    private String toJson(Object value) {
        try { return value == null ? null : objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { return null; }
    }

    private static int[][] copy(int[][] grid) {
        return Arrays.stream(grid).map(int[]::clone).toArray(int[][]::new);
    }

    private static String encode(int[][] grid) {
        StringBuilder result = new StringBuilder(81);
        for (int[] row : grid) {
            for (int value : row) {
                if (value == 0) result.append('.');
                else result.append(value);
            }
        }
        return result.toString();
    }
}