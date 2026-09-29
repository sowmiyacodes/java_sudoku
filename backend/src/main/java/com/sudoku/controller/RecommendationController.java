package com.sudoku.controller;

import com.sudoku.dto.RecommendationResponseDto;
import com.sudoku.model.User;
import com.sudoku.repository.UserRepository;
import com.sudoku.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/players")
public class RecommendationController {

    private final UserRepository userRepository;
    private final RecommendationService recommendationService;

    public RecommendationController(
            UserRepository userRepository,
            RecommendationService recommendationService
    ) {
        this.userRepository = userRepository;
        this.recommendationService = recommendationService;
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @GetMapping("/me/recommendations")
    public ResponseEntity<RecommendationResponseDto> getMyRecommendations(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(recommendationService.getLatestRecommendation(user.getId()));
    }

    @GetMapping("/{playerId}/recommendations")
    public ResponseEntity<RecommendationResponseDto> getPlayerRecommendations(
            @PathVariable Long playerId,
            Authentication authentication
    ) {
        User user = resolveAuthenticatedUser(authentication);
        if (!user.getId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to another player's recommendations is restricted");
        }
        return ResponseEntity.ok(recommendationService.getLatestRecommendation(playerId));
    }

    @PostMapping("/me/recommendations/refresh")
    public ResponseEntity<RecommendationResponseDto> refreshMyRecommendations(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(recommendationService.generateRecommendation(user.getId()));
    }
}
