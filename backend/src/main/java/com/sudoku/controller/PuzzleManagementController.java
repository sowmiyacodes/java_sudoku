package com.sudoku.controller;

import com.sudoku.dto.PuzzleManagementRequest;
import com.sudoku.dto.PuzzleRecordResponse;
import com.sudoku.service.PuzzleManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/puzzles/manage")
public class PuzzleManagementController {

    private final PuzzleManagementService puzzleService;

    public PuzzleManagementController(PuzzleManagementService puzzleService) {
        this.puzzleService = puzzleService;
    }

    @GetMapping
    public List<PuzzleRecordResponse> list() { return puzzleService.list(); }

    @GetMapping("/analytics")
    public Map<String, Object> analytics() { return puzzleService.analytics(); }

    @GetMapping("/{puzzleId}")
    public PuzzleRecordResponse get(@PathVariable String puzzleId) { return puzzleService.get(puzzleId); }

    @PostMapping
    public ResponseEntity<PuzzleRecordResponse> create(@RequestBody PuzzleManagementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(puzzleService.create(request));
    }

    @PutMapping("/{puzzleId}")
    public PuzzleRecordResponse update(@PathVariable String puzzleId, @RequestBody PuzzleManagementRequest request) {
        return puzzleService.update(puzzleId, request);
    }

    @DeleteMapping("/{puzzleId}")
    public ResponseEntity<Void> delete(@PathVariable String puzzleId) {
        puzzleService.delete(puzzleId);
        return ResponseEntity.noContent().build();
    }
}