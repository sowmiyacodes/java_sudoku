package com.sudoku;

import com.sudoku.dto.LeaderboardEntryResponse;
import com.sudoku.dto.LeaderboardPlayerStats;
import com.sudoku.dto.PublicPlayerProfileResponse;
import com.sudoku.dto.ScoreHistoryDto;
import com.sudoku.model.LeaderboardScore;
import com.sudoku.model.User;
import com.sudoku.repository.LeaderboardScoreRepository;
import com.sudoku.repository.UserRepository;
import com.sudoku.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeaderboardServiceTest {

    @Mock
    private LeaderboardScoreRepository leaderboardScoreRepository;

    @Mock
    private UserRepository userRepository;

    private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        leaderboardService = new LeaderboardService(leaderboardScoreRepository, userRepository);
    }

    @Test
    @DisplayName("Leaderboard ranking orders by total points, then completed games, then deterministic tie-break")
    void testLeaderboardRankingAndTieBreak() {
        // Player 1: 3000 pts, 2 games, best time 200s, userId 1
        LeaderboardPlayerStats p1 = new LeaderboardPlayerStats(1L, "ace", "Ace Pilot", 3000L, 2L, 95.0, 200L);
        // Player 2: 2500 pts, 3 games, best time 180s, userId 2
        LeaderboardPlayerStats p2 = new LeaderboardPlayerStats(2L, "star", "Star Voyager", 2500L, 3L, 90.0, 180L);
        // Player 3: 2500 pts, 2 games, best time 150s, userId 3 (fewer games than p2)
        LeaderboardPlayerStats p3 = new LeaderboardPlayerStats(3L, "nova", "Nova", 2500L, 2L, 92.0, 150L);
        // Player 4: 2500 pts, 2 games, best time 210s, userId 4 (equal games to p3, but slower best time)
        LeaderboardPlayerStats p4 = new LeaderboardPlayerStats(4L, "comet", "Comet", 2500L, 2L, 88.0, 210L);
        // Player 5: 2500 pts, 2 games, best time 210s, userId 5 (equal points, games, and time to p4 -> tie-break by userId)
        LeaderboardPlayerStats p5 = new LeaderboardPlayerStats(5L, "orion", "Orion", 2500L, 2L, 88.0, 210L);

        when(leaderboardScoreRepository.findAllTimeStats()).thenReturn(List.of(p1, p2, p3, p4, p5));

        List<LeaderboardEntryResponse> result = leaderboardService.getLeaderboard("all-time", null);

        assertEquals(5, result.size());
        assertEquals(1, result.get(0).rank());
        assertEquals("ace", result.get(0).username());
        assertEquals(3000L, result.get(0).totalPoints());

        assertEquals(2, result.get(1).rank());
        assertEquals("star", result.get(1).username());
        assertEquals(3L, result.get(1).gamesCompleted());

        assertEquals(3, result.get(2).rank());
        assertEquals("nova", result.get(2).username());
        assertEquals(150L, result.get(2).bestTime());

        assertEquals(4, result.get(3).rank());
        assertEquals("comet", result.get(3).username());

        assertEquals(5, result.get(4).rank());
        assertEquals("orion", result.get(4).username());
    }

    @Test
    @DisplayName("Period filter calls correct repository methods for weekly and daily periods")
    void testPeriodFilters() {
        when(leaderboardScoreRepository.findPeriodStats(any(LocalDateTime.class))).thenReturn(List.of());

        // Test daily
        leaderboardService.getLeaderboard("daily", 10);
        ArgumentCaptor<LocalDateTime> dailyCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(leaderboardScoreRepository).findPeriodStats(dailyCaptor.capture());
        assertTrue(dailyCaptor.getValue().isBefore(LocalDateTime.now()));
        assertTrue(dailyCaptor.getValue().isAfter(LocalDateTime.now().minusHours(25)));

        // Test weekly
        leaderboardService.getLeaderboard("weekly", 10);
        ArgumentCaptor<LocalDateTime> weeklyCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(leaderboardScoreRepository, times(2)).findPeriodStats(weeklyCaptor.capture());
        assertTrue(weeklyCaptor.getValue().isBefore(LocalDateTime.now().minusDays(6)));
    }

    @Test
    @DisplayName("Public profile returns stats and excludes sensitive account details")
    void testPublicPlayerProfile_HidesPrivateDetails() {
        User user = new User();
        user.setId(7L);
        user.setUsername("secretagent");
        user.setDisplayName("Agent 007");
        user.setEmail("classified@agency.gov");
        user.setPasswordHash("$2a$10$hashedsecretpassword");

        LeaderboardPlayerStats stats = new LeaderboardPlayerStats(7L, "secretagent", "Agent 007", 5000L, 4L, 98.0, 120L);

        when(userRepository.findByUsernameIgnoreCase("secretagent")).thenReturn(Optional.of(user));
        when(leaderboardScoreRepository.findAllTimeStats()).thenReturn(List.of(stats));
        when(leaderboardScoreRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());

        PublicPlayerProfileResponse profile = leaderboardService.getPublicPlayerProfile("secretagent");

        assertNotNull(profile);
        assertEquals("secretagent", profile.username());
        assertEquals("Agent 007", profile.displayName());
        assertEquals(1, profile.allTimeRank());
        assertEquals(5000L, profile.totalPoints());
        assertEquals(4L, profile.gamesCompleted());
        assertEquals(98.0, profile.averageAccuracy());
        assertEquals(120L, profile.bestTime());

        // Verify PublicPlayerProfileResponse does NOT expose email or password
        assertFalse(profile.toString().contains("classified@agency.gov"));
        assertFalse(profile.toString().contains("hashedsecretpassword"));
    }

    @Test
    @DisplayName("Private history returns user-specific score history")
    void testPrivateHistory() {
        User user = new User();
        user.setUsername("pilot1");

        when(userRepository.findByUsernameIgnoreCase("pilot1")).thenReturn(Optional.of(user));
        when(leaderboardScoreRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());

        List<ScoreHistoryDto> history = leaderboardService.getPrivateUserHistory("pilot1");
        assertNotNull(history);
        verify(leaderboardScoreRepository).findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
