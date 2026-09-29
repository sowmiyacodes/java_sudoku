package com.sudoku.controller;

import com.sudoku.model.MLPredictionLog;
import com.sudoku.repository.MLPredictionLogRepository;
import com.sudoku.service.MLPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/ml")
public class AdminMLController {

    private final MLPredictionService mlService;
    private final MLPredictionLogRepository logRepository;

    public AdminMLController(MLPredictionService mlService, MLPredictionLogRepository logRepository) {
        this.mlService = mlService;
        this.logRepository = logRepository;
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        List<Map<String, Object>> models = mlService.fetchModels();
        List<Map<String, Object>> experiments = mlService.fetchExperiments();
        Map<String, Object> datasets = mlService.fetchDatasets();
        long totalPredictions = logRepository.count();

        long activeCount = models.stream()
                .filter(m -> "ACTIVE".equalsIgnoreCase((String) m.get("status")))
                .count();

        double latestAccuracy = 0.0;
        double latestF1 = 0.0;
        if (!models.isEmpty()) {
            Object accObj = models.get(0).get("accuracy");
            Object f1Obj = models.get(0).get("f1");
            if (accObj instanceof Number n) latestAccuracy = n.doubleValue();
            if (f1Obj instanceof Number n) latestF1 = n.doubleValue();
        }

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("active_models", activeCount);
        overview.put("total_models", models.size());
        overview.put("total_training_runs", experiments.size());
        overview.put("latest_accuracy", latestAccuracy);
        overview.put("latest_f1", latestF1);
        overview.put("total_logged_predictions", totalPredictions);
        overview.put("last_training_date", experiments.isEmpty() ? "N/A" : experiments.get(0).get("timestamp"));
        overview.put("status", "HEALTHY");

        return ResponseEntity.ok(overview);
    }

    @GetMapping("/models")
    public ResponseEntity<List<Map<String, Object>>> getModels() {
        return ResponseEntity.ok(mlService.fetchModels());
    }

    @PostMapping("/models/{modelId}/activate")
    public ResponseEntity<Map<String, Object>> activateModel(@PathVariable String modelId) {
        boolean ok = mlService.activateModel(modelId);
        return ResponseEntity.ok(Map.of("success", ok, "model_id", modelId));
    }

    @GetMapping("/experiments")
    public ResponseEntity<List<Map<String, Object>>> getExperiments() {
        return ResponseEntity.ok(mlService.fetchExperiments());
    }

    @GetMapping("/datasets")
    public ResponseEntity<Map<String, Object>> getDatasets() {
        return ResponseEntity.ok(mlService.fetchDatasets());
    }

    @GetMapping("/predictions")
    public ResponseEntity<List<MLPredictionLog>> getPredictions() {
        List<MLPredictionLog> logs = logRepository.findAll();
        // Return latest 50 logs
        int start = Math.max(0, logs.size() - 50);
        List<MLPredictionLog> sub = new ArrayList<>(logs.subList(start, logs.size()));
        Collections.reverse(sub);
        return ResponseEntity.ok(sub);
    }

    @PostMapping("/train/{modelType}")
    public ResponseEntity<Map<String, Object>> trainModel(@PathVariable String modelType) {
        Map<String, Object> res = mlService.triggerTraining(modelType);
        return ResponseEntity.ok(res);
    }
}
