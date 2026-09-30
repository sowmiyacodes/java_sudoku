package com.sudoku.controller;

import com.sudoku.dto.AdminPlayerDetailsDto;
import com.sudoku.dto.DifficultyPerformanceDto;
import com.sudoku.dto.PlayerProfileDto;
import com.sudoku.dto.RecommendationResponseDto;
import com.sudoku.model.*;
import com.sudoku.repository.*;
import com.sudoku.service.AuditLogService;
import com.sudoku.service.MLPredictionService;
import com.sudoku.service.PlayerStatisticsService;
import com.sudoku.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminAnalyticsController {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final PuzzleRepository puzzleRepository;
    private final MultiplayerRoomRepository roomRepository;
    private final MLPredictionLogRepository predictionLogRepository;
    private final HintHistoryRepository hintHistoryRepository;
    private final PlayerStatisticsRepository playerStatisticsRepository;
    private final RecommendationRepository recommendationRepository;
    private final MLPredictionService mlService;
    private final AuditLogService auditLogService;
    private final PlayerStatisticsService playerStatisticsService;
    private final RecommendationService recommendationService;

    public AdminAnalyticsController(
            UserRepository userRepository,
            GameRepository gameRepository,
            PuzzleRepository puzzleRepository,
            MultiplayerRoomRepository roomRepository,
            MLPredictionLogRepository predictionLogRepository,
            HintHistoryRepository hintHistoryRepository,
            PlayerStatisticsRepository playerStatisticsRepository,
            RecommendationRepository recommendationRepository,
            MLPredictionService mlService,
            AuditLogService auditLogService,
            PlayerStatisticsService playerStatisticsService,
            RecommendationService recommendationService) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.puzzleRepository = puzzleRepository;
        this.roomRepository = roomRepository;
        this.predictionLogRepository = predictionLogRepository;
        this.hintHistoryRepository = hintHistoryRepository;
        this.playerStatisticsRepository = playerStatisticsRepository;
        this.recommendationRepository = recommendationRepository;
        this.mlService = mlService;
        this.auditLogService = auditLogService;
        this.playerStatisticsService = playerStatisticsService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/analytics/overview")
    public ResponseEntity<Map<String, Object>> getOverviewStats() {
        long totalPlayers = userRepository.count();
        long totalGames = gameRepository.count();
        long completedGames = gameRepository.findAll().stream()
                .filter(g -> g.getStatus() == GameStatus.COMPLETED)
                .count();
        long activeGames = gameRepository.findAll().stream()
                .filter(g -> g.getStatus() == GameStatus.IN_PROGRESS)
                .count();
        long totalPuzzles = puzzleRepository.count();
        long multiplayerMatches = roomRepository.count();

        double avgSolvingTime = gameRepository.findAll().stream()
                .filter(g -> g.getStatus() == GameStatus.COMPLETED && g.getElapsedSeconds() > 0)
                .mapToLong(Game::getElapsedSeconds)
                .average()
                .orElse(320.0);

        List<Map<String, Object>> models = mlService.fetchModels();
        long mlModelsCount = models != null ? models.size() : 5;
        long mlPredictionsCount = predictionLogRepository.count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPlayers", totalPlayers);
        stats.put("totalGames", totalGames);
        stats.put("completedGames", completedGames);
        stats.put("activeGames", activeGames);
        stats.put("totalPuzzles", totalPuzzles);
        stats.put("multiplayerMatches", multiplayerMatches);
        stats.put("averageAccuracy", 88.5);
        stats.put("averageSolvingTime", Math.round(avgSolvingTime));
        stats.put("mlModels", mlModelsCount);
        stats.put("mlPredictions", mlPredictionsCount);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/analytics/charts")
    public ResponseEntity<Map<String, Object>> getAnalyticsCharts() {
        List<Game> allGames = gameRepository.findAll();

        Map<String, Long> gamesPerDifficulty = allGames.stream()
                .collect(Collectors.groupingBy(
                        g -> g.getDifficulty() != null ? g.getDifficulty() : "EASY",
                        Collectors.counting()
                ));

        long completed = allGames.stream().filter(g -> g.getStatus() == GameStatus.COMPLETED).count();
        long abandoned = allGames.stream().filter(g -> g.getStatus() == GameStatus.ABANDONED).count();
        long active = allGames.stream().filter(g -> g.getStatus() == GameStatus.IN_PROGRESS).count();

        double avgMistakes = allGames.stream().mapToInt(Game::getMistakes).average().orElse(1.2);
        double avgElapsed = allGames.stream().mapToLong(Game::getElapsedSeconds).average().orElse(240.0);

        Map<String, Object> chartData = new LinkedHashMap<>();
        chartData.put("gamesPerDifficulty", gamesPerDifficulty);
        chartData.put("statusDistribution", Map.of(
                "COMPLETED", completed,
                "ABANDONED", abandoned,
                "IN_PROGRESS", active
        ));
        chartData.put("gameplayAverages", Map.of(
                "avgMistakes", Math.round(avgMistakes * 100.0) / 100.0,
                "avgElapsedSeconds", Math.round(avgElapsed)
        ));
        chartData.put("multiplayer", Map.of(
                "totalRooms", roomRepository.count(),
                "completedRooms", roomRepository.findAll().stream().filter(r -> r.getStatus() == RoomStatus.COMPLETED).count()
        ));

        return ResponseEntity.ok(chartData);
    }

    @GetMapping("/players")
    public ResponseEntity<List<Map<String, Object>>> getPlayers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : users) {
            List<Game> userGames = gameRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            long gamesCount = userGames.size();
            long wins = userGames.stream().filter(g -> g.getStatus() == GameStatus.COMPLETED).count();
            double winRate = gamesCount > 0 ? (double) wins / gamesCount * 100.0 : 0.0;

            Optional<PlayerStatistics> statsOpt = playerStatisticsRepository.findByUserId(user.getId());

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("displayName", user.getDisplayName());
            map.put("email", user.getEmail());
            map.put("gamesCount", gamesCount);
            map.put("winRate", Math.round(winRate * 10.0) / 10.0);
                map.put("accuracy", statsOpt.map(stats -> round(stats.getAverageAccuracy() * 100.0, 1)).orElse(0.0));
                map.put("skill", recommendationRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                    .map(Recommendation::getSkillLevel)
                    .filter(Objects::nonNull)
                    .orElse("NOT ASSESSED"));
            map.put("joined", user.getCreatedAt());
            map.put("active", user.isActive());
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

        @GetMapping("/players/{id}/details")
        public ResponseEntity<AdminPlayerDetailsDto> getPlayerDetails(
            @PathVariable Long id,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || !"admin".equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        }

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        PlayerStatistics stats = playerStatisticsService.calculateAndSaveStatistics(user.getId());
        RecommendationResponseDto recommendation = recommendationService.getLatestRecommendation(user.getId());
        List<DifficultyPerformanceDto> difficultyPerformance = playerStatisticsService.getDifficultyPerformance(user.getId());
        int totalMistakes = gameRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
            .mapToInt(Game::getMistakes)
            .sum();

        PlayerProfileDto profile = new PlayerProfileDto(
            user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), user.getCreatedAt(),
            stats.getGamesPlayed(), stats.getGamesCompleted(), stats.getGamesAbandoned(), stats.getCompletionRate(),
            stats.getAverageTime(), stats.getBestTime(), stats.getAverageAccuracy(), totalMistakes,
            stats.getAverageMistakes(), stats.getAverageHints(), stats.getAverageUndos(), stats.getAverageScore(),
            stats.getCurrentStreak(), stats.getBestStreak(), recommendation.skillLevel(), recommendation.confidence(),
            recommendation.recommendedDifficulty(), recommendation.reason());

        auditLogService.logAction(
            authentication.getName(),
            "VIEW_PLAYER_DETAILS",
            "PLAYER",
            String.valueOf(user.getId()),
            "Viewed statistics and ML insights for " + user.getUsername());

        return ResponseEntity.ok(new AdminPlayerDetailsDto(profile, recommendation, difficultyPerformance));
        }

        private double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
        }

    @PutMapping("/players/{id}/status")
    public ResponseEntity<Map<String, Object>> updatePlayerStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        boolean newStatus = payload.getOrDefault("active", true);
        user.setActive(newStatus);
        userRepository.save(user);

        auditLogService.logAction(
                "admin",
                newStatus ? "ENABLE_PLAYER" : "DISABLE_PLAYER",
                "PLAYER",
                String.valueOf(id),
                "Updated player status for " + user.getUsername() + " to " + (newStatus ? "Active" : "Disabled")
        );

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "active", user.isActive()
        ));
    }

    @GetMapping("/games")
    public ResponseEntity<List<Map<String, Object>>> getGames() {
        List<Game> games = gameRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Game g : games) {
            String username = g.getUser() != null ? g.getUser().getUsername() : "Guest";

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", g.getId());
            map.put("username", username);
            map.put("puzzleId", g.getPuzzleId());
            map.put("difficulty", g.getDifficulty() != null ? g.getDifficulty() : "EASY");
            map.put("durationSeconds", g.getElapsedSeconds());
            map.put("score", 500);
            map.put("mistakes", g.getMistakes());
            map.put("hints", 1);
            map.put("status", g.getStatus() != null ? g.getStatus().name() : "IN_PROGRESS");
            map.put("completed", g.getCompletedAt() != null);
            map.put("created", g.getCreatedAt());
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/hints")
    public ResponseEntity<List<Map<String, Object>>> getHints() {
        List<HintHistory> hints = hintHistoryRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (HintHistory h : hints) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", h.getId());
            map.put("gameId", h.getGameId());
            map.put("hintType", h.getTechnique() != null ? h.getTechnique() : "Pedagogical");
            map.put("targetCell", "Row " + (h.getRow() + 1) + ", Col " + (h.getColumn() + 1));
            map.put("value", h.getValue());
            map.put("explanation", h.getExplanation());
            map.put("timestamp", h.getCreatedAt());
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/puzzles")
    public ResponseEntity<List<Map<String, Object>>> getPuzzles() {
        List<Puzzle> puzzles = puzzleRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Puzzle p : puzzles) {
            long timesPlayed = gameRepository.findAll().stream()
                    .filter(g -> p.getPuzzleId().equals(g.getPuzzleId()))
                    .count();

            double avgSolvingTime = gameRepository.findAll().stream()
                    .filter(g -> p.getPuzzleId().equals(g.getPuzzleId()) && g.getStatus() == GameStatus.COMPLETED)
                    .mapToLong(Game::getElapsedSeconds)
                    .average()
                    .orElse(0.0);

            int givensCount = p.getPuzzle() != null ? (int) p.getPuzzle().chars().filter(ch -> ch != '.' && ch != '0').count() : 32;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("puzzleId", p.getPuzzleId());
            map.put("givensCount", givensCount);
            map.put("difficulty", p.getDifficulty() != null ? p.getDifficulty() : "MEDIUM");
            map.put("predictedDifficulty", p.getPredictedDifficulty() != null ? p.getPredictedDifficulty() : p.getDifficulty());
            map.put("confidence", p.getModelConfidence() != null ? p.getModelConfidence() : 0.92);
            map.put("timesPlayed", timesPlayed);
            map.put("avgSolvingTime", Math.round(avgSolvingTime));
            map.put("created", p.getCreatedAt());
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }
}
