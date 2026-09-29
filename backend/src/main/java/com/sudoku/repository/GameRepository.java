package com.sudoku.repository;

import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByStatusOrderByUpdatedAtDesc(GameStatus status);

    @Query("SELECT g FROM Game g WHERE g.status IN :statuses ORDER BY g.updatedAt DESC")
    List<Game> findByStatusInOrderByUpdatedAtDesc(@Param("statuses") List<GameStatus> statuses);

    List<Game> findByUserIdAndStatusInOrderByUpdatedAtDesc(Long userId, List<GameStatus> statuses);

    List<Game> findByUserIdIsNullAndStatusInOrderByUpdatedAtDesc(List<GameStatus> statuses);

    List<Game> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Game> findByUserIdOrderByStartedAtDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, GameStatus status);

    List<Game> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    List<Game> findByUserIdAndDifficultyIgnoreCaseOrderByCreatedAtDesc(Long userId, String difficulty);

    default Optional<Game> findLatestResumable() {
        List<Game> resumable = findByStatusInOrderByUpdatedAtDesc(List.of(GameStatus.IN_PROGRESS, GameStatus.PAUSED));
        return resumable.isEmpty() ? Optional.empty() : Optional.of(resumable.get(0));
    }

    default Optional<Game> findLatestResumableByUser(Long userId) {
        List<Game> resumable = userId != null
                ? findByUserIdAndStatusInOrderByUpdatedAtDesc(userId, List.of(GameStatus.IN_PROGRESS, GameStatus.PAUSED))
                : findByUserIdIsNullAndStatusInOrderByUpdatedAtDesc(List.of(GameStatus.IN_PROGRESS, GameStatus.PAUSED));
        return resumable.isEmpty() ? Optional.empty() : Optional.of(resumable.get(0));
    }
}
