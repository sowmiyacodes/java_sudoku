package com.sudoku.controller;

import com.sudoku.dto.PlayerProfileDto;
import com.sudoku.dto.RecommendationResponseDto;
import com.sudoku.dto.UpdateProfileRequest;
import com.sudoku.model.Game;
import com.sudoku.model.PlayerStatistics;
import com.sudoku.model.User;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.UserRepository;
import com.sudoku.service.PlayerStatisticsService;
import com.sudoku.service.RecommendationService;
import com.sudoku.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/players")
public class PlayerProfileController {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final PlayerStatisticsService statisticsService;
    private final RecommendationService recommendationService;
    private final UserService userService;

    public PlayerProfileController(
            UserRepository userRepository,
            GameRepository gameRepository,
            PlayerStatisticsService statisticsService,
            RecommendationService recommendationService,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.statisticsService = statisticsService;
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<PlayerProfileDto> getMyProfile(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(assembleProfile(user));
    }

    @GetMapping("/{playerId}/profile")
    public ResponseEntity<PlayerProfileDto> getPlayerProfile(
            @PathVariable Long playerId,
            Authentication authentication
    ) {
        User user = resolveAuthenticatedUser(authentication);
        if (!user.getId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to another player's private profile is restricted");
        }
        return ResponseEntity.ok(assembleProfile(user));
    }

    @PutMapping("/{playerId}/profile")
    public ResponseEntity<PlayerProfileDto> updatePlayerProfile(
            @PathVariable Long playerId,
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        User user = resolveAuthenticatedUser(authentication);
        if (!user.getId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify another player's profile");
        }
        User updated = userService.updateProfile(user.getUsername(), request);
        return ResponseEntity.ok(assembleProfile(updated));
    }

    @DeleteMapping("/{playerId}/profile")
    public ResponseEntity<Map<String, String>> deletePlayerProfile(
            @PathVariable Long playerId,
            Authentication authentication
    ) {
        User user = resolveAuthenticatedUser(authentication);
        if (!user.getId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete another player's profile");
        }
        userService.deleteUser(playerId);
        return ResponseEntity.ok(Map.of("message", "Player profile successfully deleted."));
    }

    private PlayerProfileDto assembleProfile(User user) {
        PlayerStatistics stats = statisticsService.calculateAndSaveStatistics(user.getId());
        RecommendationResponseDto rec = recommendationService.getLatestRecommendation(user.getId());

        List<Game> games = gameRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        int totalMistakes = games.stream().mapToInt(Game::getMistakes).sum();

        return new PlayerProfileDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getCreatedAt(),
                stats.getGamesPlayed(),
                stats.getGamesCompleted(),
                stats.getGamesAbandoned(),
                stats.getCompletionRate(),
                stats.getAverageTime(),
                stats.getBestTime(),
                stats.getAverageAccuracy(),
                totalMistakes,
                stats.getAverageMistakes(),
                stats.getAverageHints(),
                stats.getAverageUndos(),
                stats.getAverageScore(),
                stats.getCurrentStreak(),
                stats.getBestStreak(),
                rec.skillLevel(),
                rec.confidence(),
                rec.recommendedDifficulty(),
                rec.reason()
        );
    }
}
