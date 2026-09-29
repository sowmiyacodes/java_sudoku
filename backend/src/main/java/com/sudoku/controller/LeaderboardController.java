package com.sudoku.controller;

import com.sudoku.dto.LeaderboardEntryResponse;
import com.sudoku.dto.PublicPlayerProfileResponse;
import com.sudoku.dto.ScoreHistoryDto;
import com.sudoku.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * Get leaderboard rankings for a period (all-time, weekly, daily).
     */
    @GetMapping
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(
            @RequestParam(defaultValue = "all-time") String period,
            @RequestParam(required = false) Integer limit
    ) {
        List<LeaderboardEntryResponse> entries = leaderboardService.getLeaderboard(period, limit);
        return ResponseEntity.ok(entries);
    }

    /**
     * Get the authenticated user's current ranking for a period.
     */
    @GetMapping("/me")
    public ResponseEntity<LeaderboardEntryResponse> getMyRank(
            @RequestParam(defaultValue = "all-time") String period,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        return leaderboardService.getUserLeaderboardEntry(authentication.getName(), period)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * View public player profile. Excludes private email and password details.
     */
    @GetMapping("/profile/{username}")
    public ResponseEntity<PublicPlayerProfileResponse> getPublicPlayerProfile(
            @PathVariable String username
    ) {
        PublicPlayerProfileResponse profile = leaderboardService.getPublicPlayerProfile(username);
        return ResponseEntity.ok(profile);
    }

    /**
     * Retrieve private completed game history for the authenticated user.
     */
    @GetMapping("/history")
    public ResponseEntity<List<ScoreHistoryDto>> getMyHistory(
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        List<ScoreHistoryDto> history = leaderboardService.getPrivateUserHistory(authentication.getName());
        return ResponseEntity.ok(history);
    }
}
