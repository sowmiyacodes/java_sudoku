package com.sudoku.service;

import com.sudoku.dto.LeaderboardEntryResponse;
import com.sudoku.dto.LeaderboardPlayerStats;
import com.sudoku.dto.PublicPlayerProfileResponse;
import com.sudoku.dto.ScoreHistoryDto;
import com.sudoku.model.LeaderboardScore;
import com.sudoku.model.User;
import com.sudoku.repository.LeaderboardScoreRepository;
import com.sudoku.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class LeaderboardService {

    private final LeaderboardScoreRepository leaderboardScoreRepository;
    private final UserRepository userRepository;

    public LeaderboardService(LeaderboardScoreRepository leaderboardScoreRepository, UserRepository userRepository) {
        this.leaderboardScoreRepository = leaderboardScoreRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retrieve leaderboard entries for the specified period: all-time, weekly, or daily.
     * Ordered deterministically by total points DESC, then completed games DESC,
     * then best completion time ASC, then user ID ASC.
     */
    public List<LeaderboardEntryResponse> getLeaderboard(String period, Integer limit) {
        List<LeaderboardPlayerStats> rawStats = fetchPeriodStats(period);
        List<LeaderboardEntryResponse> ranked = new ArrayList<>();

        int count = rawStats.size();
        int max = (limit != null && limit > 0) ? Math.min(limit, count) : count;

        for (int i = 0; i < max; i++) {
            LeaderboardPlayerStats stats = rawStats.get(i);
            int rank = i + 1;
            ranked.add(LeaderboardEntryResponse.from(rank, stats));
        }

        return ranked;
    }

    /**
     * Get the leaderboard entry (including rank) of a specific user for a period.
     */
    public Optional<LeaderboardEntryResponse> getUserLeaderboardEntry(String username, String period) {
        List<LeaderboardEntryResponse> allEntries = getLeaderboard(period, null);
        return allEntries.stream()
                .filter(entry -> entry.username().equalsIgnoreCase(username))
                .findFirst();
    }

    /**
     * Get a player's public profile statistics.
     * Restricts private account details (email, password hash, etc.) and private game moves.
     */
    public PublicPlayerProfileResponse getPublicPlayerProfile(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found: " + username));

        List<LeaderboardEntryResponse> allTimeRanked = getLeaderboard("all-time", null);
        Optional<LeaderboardEntryResponse> userEntryOpt = allTimeRanked.stream()
                .filter(e -> e.userId().equals(user.getId()))
                .findFirst();

        Integer allTimeRank = userEntryOpt.map(LeaderboardEntryResponse::rank).orElse(null);
        long totalPoints = userEntryOpt.map(LeaderboardEntryResponse::totalPoints).orElse(0L);
        long gamesCompleted = userEntryOpt.map(LeaderboardEntryResponse::gamesCompleted).orElse(0L);
        double averageAccuracy = userEntryOpt.map(LeaderboardEntryResponse::averageAccuracy).orElse(0.0);
        long bestTime = userEntryOpt.map(LeaderboardEntryResponse::bestTime).orElse(0L);

        List<LeaderboardScore> recentScores = leaderboardScoreRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());
        List<ScoreHistoryDto> recentScoreDtos = recentScores.stream()
                .map(ScoreHistoryDto::fromEntity)
                .toList();

        return new PublicPlayerProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getCreatedAt(),
                allTimeRank,
                totalPoints,
                gamesCompleted,
                averageAccuracy,
                bestTime,
                recentScoreDtos
        );
    }

    /**
     * Get the private completed game score history for the authenticated user.
     */
    public List<ScoreHistoryDto> getPrivateUserHistory(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<LeaderboardScore> scores = leaderboardScoreRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return scores.stream()
                .map(ScoreHistoryDto::fromEntity)
                .toList();
    }

    private List<LeaderboardPlayerStats> fetchPeriodStats(String period) {
        if (period == null) {
            return leaderboardScoreRepository.findAllTimeStats();
        }

        String normalized = period.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        return switch (normalized) {
            case "daily", "day", "24h" -> leaderboardScoreRepository.findPeriodStats(now.minusHours(24));
            case "weekly", "week", "7d" -> leaderboardScoreRepository.findPeriodStats(now.minusDays(7));
            default -> leaderboardScoreRepository.findAllTimeStats();
        };
    }
}
