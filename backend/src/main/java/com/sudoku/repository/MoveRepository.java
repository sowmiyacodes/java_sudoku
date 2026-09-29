package com.sudoku.repository;

import com.sudoku.model.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {

    List<Move> findByGameIdOrderByMoveNumberAsc(Long gameId);

    // Get the latest active (non-undone) move for undo
    Optional<Move> findTopByGameIdAndUndoneFalseOrderByMoveNumberDesc(Long gameId);

    // Get the earliest undone move for redo
    Optional<Move> findTopByGameIdAndUndoneTrueOrderByMoveNumberAsc(Long gameId);

    // Check if can undo
    boolean existsByGameIdAndUndoneFalse(Long gameId);

    // Check if can redo
    boolean existsByGameIdAndUndoneTrue(Long gameId);

    // Count non-undone moves to determine next move number
    long countByGameId(Long gameId);

    // Delete any redo history when a new move is made after an undo
    @Modifying
    @Query("DELETE FROM Move m WHERE m.gameId = :gameId AND m.undone = true")
    void deleteUndoneMovesByGameId(Long gameId);

    // Delete all moves for a game
    void deleteByGameId(Long gameId);
}
