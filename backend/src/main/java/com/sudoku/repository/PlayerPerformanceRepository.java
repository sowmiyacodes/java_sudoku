package com.sudoku.repository;

import com.sudoku.model.PlayerPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerPerformanceRepository extends JpaRepository<PlayerPerformance, Long> {
    List<PlayerPerformance> findAllByOrderByCreatedAtDesc();
    List<PlayerPerformance> findByGameIdOrderByCreatedAtDesc(Long gameId);

    @Query("SELECT p FROM PlayerPerformance p, Game g WHERE p.gameId = g.id AND g.user.id = :userId ORDER BY p.createdAt DESC")
    List<PlayerPerformance> findByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM PlayerPerformance p, Game g WHERE p.gameId = g.id AND g.user IS NULL ORDER BY p.createdAt DESC")
    List<PlayerPerformance> findGuestPerformances();
}
