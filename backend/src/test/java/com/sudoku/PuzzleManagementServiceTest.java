package com.sudoku;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudoku.dto.PuzzleManagementRequest;
import com.sudoku.dto.PuzzleRecordResponse;
import com.sudoku.model.Puzzle;
import com.sudoku.repository.MLPredictionLogRepository;
import com.sudoku.repository.PuzzleRepository;
import com.sudoku.service.MLPredictionService;
import com.sudoku.service.PuzzleManagementService;
import com.sudoku.service.SudokuSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PuzzleManagementServiceTest {

    private static final String PUZZLE = "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79";
    private static final String SOLUTION = "534678912672195348198342567859761423426853791713924856961537284287419635345286179";

    private PuzzleRepository puzzleRepository;
    private SudokuSolver solver;
    private MLPredictionService mlPredictionService;
    private PuzzleManagementService service;

    @BeforeEach
    void setUp() {
        puzzleRepository = mock(PuzzleRepository.class);
        solver = new SudokuSolver();
        mlPredictionService = new MLPredictionService(new RestTemplateBuilder(), mock(MLPredictionLogRepository.class), new ObjectMapper());
        ReflectionTestUtils.setField(mlPredictionService, "mlApiUrl", "http://localhost:8001");
        service = new PuzzleManagementService(puzzleRepository, solver, mlPredictionService, new ObjectMapper());
    }

    @Test
    void rejectsMalformedPuzzleBeforePrediction() {
        PuzzleManagementRequest request = new PuzzleManagementRequest("123", "Easy", "test", null, true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.create(request));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(puzzleRepository);
    }

    @Test
    void savesUniquePuzzleWithDifficultyPredictionAndFeatures() {
        when(puzzleRepository.save(any(Puzzle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PuzzleRecordResponse response = service.create(new PuzzleManagementRequest(PUZZLE, null, "benchmark", 2.4, true));

        assertEquals("MEDIUM", response.difficulty());
        assertEquals("MEDIUM", response.predictedDifficulty());
        assertEquals(0.70, response.modelConfidence());
        assertNotNull(response.topFactorsJson());
        verify(puzzleRepository).save(any(Puzzle.class));
    }
}