package com.barlog.barlog_back.workout;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    @EntityGraph(attributePaths = {"sets", "sets.exercise"})
    List<WorkoutSession> findByUser_UserIdOrderByStartedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"sets", "sets.exercise"})
    List<WorkoutSession> findByUser_UserIdAndStartedAtGreaterThanEqualOrderByStartedAtAsc(UUID userId, Instant from);

    Optional<WorkoutSession> findBySessionIdAndUser_UserId(UUID sessionId, UUID userId);

    Optional<WorkoutSession> findFirstByUser_UserIdAndEndedAtIsNullOrderByStartedAtDesc(UUID userId);

    boolean existsByUser_UserIdAndEndedAtIsNull(UUID userId);
}
