package com.sudoku.repository;

import com.sudoku.model.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

    List<RoomParticipant> findByRoomIdOrderByJoinedAtAsc(Long roomId);

    Optional<RoomParticipant> findByRoomIdAndUserId(Long roomId, Long userId);

    long countByRoomIdAndLeftAtIsNull(Long roomId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
}
