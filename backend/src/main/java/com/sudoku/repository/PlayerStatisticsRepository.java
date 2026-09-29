package com.sudoku.repository;

import com.sudoku.model.PlayerStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {
    Optional<PlayerStatistics> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
