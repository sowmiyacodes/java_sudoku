package com.sudoku.controller;

import com.sudoku.service.MLPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ml")
public class PlayerMLController {

    private final MLPredictionService mlService;

    public PlayerMLController(MLPredictionService mlService) {
        this.mlService = mlService;
    }

    @PostMapping("/predict/difficulty")
    public ResponseEntity<Map<String, Object>> predictDifficulty(@RequestBody Map<String, String> body) {
        String puzzle = body.getOrDefault("puzzle", "");
        return ResponseEntity.ok(mlService.predictDifficulty(puzzle));
    }

    @PostMapping("/predict/completion")
    public ResponseEntity<Map<String, Object>> predictCompletion(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(mlService.predictCompletion(body));
    }

    @PostMapping("/predict/hint")
    public ResponseEntity<Map<String, Object>> predictHint(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(mlService.predictHint(body));
    }
}
