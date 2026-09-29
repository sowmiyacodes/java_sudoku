package com.sudoku.controller;

import com.sudoku.dto.DifficultyPerformanceDto;
import com.sudoku.dto.PlayerStatisticsDto;
import com.sudoku.model.PlayerStatistics;
import com.sudoku.model.User;
import com.sudoku.repository.UserRepository;
import com.sudoku.service.PlayerStatisticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerStatisticsController {

    private final UserRepository userRepository;
    private final PlayerStatisticsService statisticsService;

    public PlayerStatisticsController(
            UserRepository userRepository,
            PlayerStatisticsService statisticsService
    ) {
        this.userRepository = userRepository;
        this.statisticsService = statisticsService;
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @GetMapping("/me/statistics")
    public ResponseEntity<PlayerStatisticsDto> getMyStatistics(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        PlayerStatistics stats = statisticsService.calculateAndSaveStatistics(user.getId());
        return ResponseEntity.ok(PlayerStatisticsDto.fromEntity(stats));
    }

    @GetMapping("/{playerId}/statistics")
    public ResponseEntity<PlayerStatisticsDto> getPlayerStatistics(
            @PathVariable Long playerId,
            Authentication authentication
    ) {
        User user = resolveAuthenticatedUser(authentication);
        if (!user.getId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to another player's private statistics is restricted");
        }
        PlayerStatistics stats = statisticsService.calculateAndSaveStatistics(playerId);
        return ResponseEntity.ok(PlayerStatisticsDto.fromEntity(stats));
    }

    @GetMapping("/me/difficulty-performance")
    public ResponseEntity<List<DifficultyPerformanceDto>> getMyDifficultyPerformance(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(statisticsService.getDifficultyPerformance(user.getId()));
    }

    @PostMapping("/me/statistics/recalculate")
    public ResponseEntity<PlayerStatisticsDto> recalculateMyStatistics(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        PlayerStatistics stats = statisticsService.calculateAndSaveStatistics(user.getId());
        return ResponseEntity.ok(PlayerStatisticsDto.fromEntity(stats));
    }
}
