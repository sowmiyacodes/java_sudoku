package com.sudoku.repository;

import com.sudoku.model.MultiplayerRoom;
import com.sudoku.model.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MultiplayerRoomRepository extends JpaRepository<MultiplayerRoom, Long> {

    Optional<MultiplayerRoom> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    boolean existsByGameId(Long gameId);

    /**
     * True when the user is (or was) a member of the room owning this game.
     * Used to grant room members read access to the shared game through the
     * existing solo read endpoints (mutations stay blocked by
     * GameService.assertNotRoomGame).
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM MultiplayerRoom r "
            + "WHERE r.game.id = :gameId AND EXISTS "
            + "(SELECT p FROM RoomParticipant p WHERE p.room.id = r.id AND p.user.id = :userId)")
    boolean existsByGameIdAndUserId(@Param("gameId") Long gameId, @Param("userId") Long userId);

    /**
     * Fetches a room while holding a pessimistic write lock on its row
     * (SELECT ... FOR UPDATE). All room mutations must go through this method
     * so concurrent moves inside one room are serialized.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM MultiplayerRoom r WHERE r.roomCode = :roomCode")
    Optional<MultiplayerRoom> findByRoomCodeForUpdate(@Param("roomCode") String roomCode);

    /** Rooms whose activity is older than the given cutoff and can be expired. */
    @Query("SELECT r FROM MultiplayerRoom r WHERE r.status IN :statuses AND r.lastActivityAt < :cutoff")
    List<MultiplayerRoom> findStaleRooms(@Param("statuses") List<RoomStatus> statuses, @Param("cutoff") LocalDateTime cutoff);
}
