package com.sudoku.repository;

import com.sudoku.model.HintHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HintHistoryRepository extends JpaRepository<HintHistory, Long> {
    List<HintHistory> findByGameIdOrderByCreatedAtDesc(Long gameId);
    long countByGameId(Long gameId);
    void deleteByGameId(Long gameId);
}
