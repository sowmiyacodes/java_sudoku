package com.sudoku.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudoku.dto.SkillPredictionRequestDto;
import com.sudoku.dto.SkillPredictionResponseDto;
import com.sudoku.model.MLPredictionLog;
import com.sudoku.model.PlayerStatistics;
import com.sudoku.repository.MLPredictionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
public class MLPredictionService {

    private static final Logger log = LoggerFactory.getLogger(MLPredictionService.class);

    private final RestTemplate restTemplate;
    private final MLPredictionLogRepository logRepository;
    private final ObjectMapper objectMapper;

    @Value("${ml.api.url:http://localhost:8001}")
    private String mlApiUrl;

    public MLPredictionService(
            RestTemplateBuilder restTemplateBuilder,
            MLPredictionLogRepository logRepository,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(1500))
                .setReadTimeout(Duration.ofMillis(2500))
                .build();
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Calls Python FastAPI /predict/skill with player features.
     */
    public SkillPredictionResponseDto predictSkill(Long userId, SkillPredictionRequestDto payload) {
        String endpoint = mlApiUrl + "/predict/skill";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SkillPredictionRequestDto> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<SkillPredictionResponseDto> response = restTemplate.postForEntity(
                    endpoint, request, SkillPredictionResponseDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                SkillPredictionResponseDto body = response.getBody();
                logToDatabase(userId, body.skillLevel(), body.confidence(), body.modelVersion(), payload);
                return body;
            }
        } catch (RestClientException ex) {
            log.warn("FastAPI ML service unreachable at {}. Reason: {}. Using heuristic evaluation.", endpoint, ex.getMessage());
        }

        return fallbackSkillPrediction(userId, payload);
    }

    /**
     * Calls Python FastAPI /predict/difficulty for a puzzle string.
     */
    public Map<String, Object> predictDifficulty(String puzzle) {
        String endpoint = mlApiUrl + "/predict/difficulty";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("puzzle", puzzle), headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (RestClientException ex) {
            log.warn("FastAPI difficulty prediction failed: {}. Falling back to heuristic.", ex.getMessage());
        }

        long emptyCells = puzzle.chars().filter(c -> c == '.' || c == '0').count();
        String diff = emptyCells > 52 ? "HARD" : (emptyCells > 40 ? "MEDIUM" : "EASY");
        return Map.of(
                "difficulty", diff,
                "confidence", 0.70,
                "model_version", "v1.0-fallback",
                "top_factors", List.of("Heuristic clue density estimation")
        );
    }

    /**
     * Calls Python FastAPI /predict/completion
     */
    public Map<String, Object> predictCompletion(Map<String, Object> payload) {
        String endpoint = mlApiUrl + "/predict/completion";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (RestClientException ex) {
            log.warn("FastAPI completion prediction failed: {}. Falling back.", ex.getMessage());
        }

        return Map.of(
                "completion_probability", 0.75,
                "predicted_completion", true,
                "confidence", 0.65,
                "top_factors", List.of("Baseline completion heuristic")
        );
    }

    /**
     * Calls Python FastAPI /predict/hint
     */
    public Map<String, Object> predictHint(Map<String, Object> payload) {
        String endpoint = mlApiUrl + "/predict/hint";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            }
        } catch (RestClientException ex) {
            log.warn("FastAPI hint prediction failed: {}. Falling back.", ex.getMessage());
        }

        return Map.of(
                "hint_type", "REGION",
                "confidence", 0.70,
                "reason", "Check the upper-left 3x3 quadrant for constrained cells.",
                "top_factors", List.of("Heuristic quadrant search")
        );
    }

    /**
     * Registry Operations
     */
    public List<Map<String, Object>> fetchModels() {
        String endpoint = mlApiUrl + "/ml/models";
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    endpoint, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception ex) {
            log.warn("Unable to fetch models from ML service: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> fetchExperiments() {
        String endpoint = mlApiUrl + "/ml/experiments";
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    endpoint, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception ex) {
            log.warn("Unable to fetch experiments from ML service: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> fetchDatasets() {
        String endpoint = mlApiUrl + "/ml/datasets";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(endpoint, Map.class);
            return response.getBody() != null ? (Map<String, Object>) response.getBody() : Collections.emptyMap();
        } catch (Exception ex) {
            log.warn("Unable to fetch datasets from ML service: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> fetchDifficultyFeatureImportance() {
        String endpoint = mlApiUrl + "/ml/difficulty/importance";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(endpoint, Map.class);
            return response.getBody() != null ? (Map<String, Object>) response.getBody() : Collections.emptyMap();
        } catch (Exception ex) {
            log.warn("Unable to fetch puzzle difficulty feature importance: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> triggerTraining(String modelType) {
        String endpoint = mlApiUrl + "/train/" + modelType.toLowerCase();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, request, Map.class);
            return response.getBody() != null ? (Map<String, Object>) response.getBody() : Map.of("status", "error");
        } catch (Exception ex) {
            log.error("Failed to trigger training for model {}: {}", modelType, ex.getMessage());
            return Map.of("status", "error", "message", ex.getMessage());
        }
    }

    public boolean activateModel(String modelId) {
        String endpoint = mlApiUrl + "/ml/models/" + modelId + "/activate";
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, null, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            log.error("Failed to activate model {}: {}", modelId, ex.getMessage());
            return false;
        }
    }

    private void logToDatabase(Long userId, String skillLevel, double confidence, String version, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            MLPredictionLog logEntry = new MLPredictionLog(userId, skillLevel, confidence, version, jsonPayload);
            logRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to record ML prediction log: {}", e.getMessage());
        }
    }

    private SkillPredictionResponseDto fallbackSkillPrediction(Long userId, SkillPredictionRequestDto req) {
        String fallbackSkill = "INTERMEDIATE";
        if (req.gamesPlayed() < 3) {
            fallbackSkill = "BEGINNER";
        } else if (req.completionRate() >= 0.85 && req.averageAccuracy() >= 0.90 && req.hardCompletionRate() >= 0.60) {
            fallbackSkill = "EXPERT";
        } else if (req.completionRate() >= 0.70 && req.averageAccuracy() >= 0.85) {
            fallbackSkill = "ADVANCED";
        } else if (req.completionRate() < 0.50 || req.averageAccuracy() < 0.75) {
            fallbackSkill = "BEGINNER";
        }

        List<String> factors = List.of(
                "Important model factor: Fallback heuristic active due to ML microservice status",
                "Important model factor: Completion rate (" + (int) (req.completionRate() * 100) + "%)",
                "Important model factor: Average accuracy (" + (int) (req.averageAccuracy() * 100) + "%)"
        );

        SkillPredictionResponseDto.ExplanationDto explanation = new SkillPredictionResponseDto.ExplanationDto(factors);
        SkillPredictionResponseDto responseDto = new SkillPredictionResponseDto(fallbackSkill, 0.70, "v1.1-fallback", explanation);

        logToDatabase(userId, fallbackSkill, 0.70, "v1.1-fallback", req);
        return responseDto;
    }
}
