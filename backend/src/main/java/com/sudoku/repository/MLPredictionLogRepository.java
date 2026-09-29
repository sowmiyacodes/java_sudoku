package com.sudoku.repository;

import com.sudoku.model.MLPredictionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MLPredictionLogRepository extends JpaRepository<MLPredictionLog, Long> {
    List<MLPredictionLog> findByUserIdOrderByPredictionTimeDesc(Long userId);
    long countByModelVersion(String modelVersion);
}
