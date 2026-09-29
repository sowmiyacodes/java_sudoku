package com.sudoku.repository;

import com.sudoku.model.PlayerPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerPreferenceRepository extends JpaRepository<PlayerPreference, Long> {
    Optional<PlayerPreference> findByUserId(Long userId);
}
