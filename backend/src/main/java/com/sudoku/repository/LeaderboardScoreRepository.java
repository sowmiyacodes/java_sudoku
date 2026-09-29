package com.sudoku.repository;

import com.sudoku.dto.LeaderboardPlayerStats;
import com.sudoku.model.LeaderboardScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardScoreRepository extends JpaRepository<LeaderboardScore, Long> {

    boolean existsByGameId(Long gameId);

    boolean existsByGameIdAndUserId(Long gameId, Long userId);

    Optional<LeaderboardScore> findByGameId(Long gameId);

    Optional<LeaderboardScore> findByGameIdAndUserId(Long gameId, Long userId);

    List<LeaderboardScore> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<LeaderboardScore> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT new com.sudoku.dto.LeaderboardPlayerStats(" +
            "s.user.id, s.user.username, s.user.displayName, " +
            "SUM(s.points), COUNT(s.id), AVG(CAST(s.accuracy AS double)), MIN(s.elapsedSeconds)) " +
            "FROM LeaderboardScore s " +
            "GROUP BY s.user.id, s.user.username, s.user.displayName " +
            "ORDER BY SUM(s.points) DESC, COUNT(s.id) DESC, MIN(s.elapsedSeconds) ASC, s.user.id ASC")
    List<LeaderboardPlayerStats> findAllTimeStats();

    @Query("SELECT new com.sudoku.dto.LeaderboardPlayerStats(" +
            "s.user.id, s.user.username, s.user.displayName, " +
            "SUM(s.points), COUNT(s.id), AVG(CAST(s.accuracy AS double)), MIN(s.elapsedSeconds)) " +
            "FROM LeaderboardScore s " +
            "WHERE s.createdAt >= :since " +
            "GROUP BY s.user.id, s.user.username, s.user.displayName " +
            "ORDER BY SUM(s.points) DESC, COUNT(s.id) DESC, MIN(s.elapsedSeconds) ASC, s.user.id ASC")
    List<LeaderboardPlayerStats> findPeriodStats(@Param("since") LocalDateTime since);

    @Query("SELECT new com.sudoku.dto.LeaderboardPlayerStats(" +
            "s.user.id, s.user.username, s.user.displayName, " +
            "SUM(s.points), COUNT(s.id), AVG(CAST(s.accuracy AS double)), MIN(s.elapsedSeconds)) " +
            "FROM LeaderboardScore s " +
            "WHERE s.user.id = :userId " +
            "GROUP BY s.user.id, s.user.username, s.user.displayName")
    Optional<LeaderboardPlayerStats> findUserAllTimeStats(@Param("userId") Long userId);
}
