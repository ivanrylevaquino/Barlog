package com.barlog.barlog_back.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, UUID> {

    Optional<ExerciseSet> findBySetIdAndSession_User_UserId(UUID setId, UUID userId);

    List<ExerciseSet> findBySession_User_UserIdOrderByCreatedAtDesc(UUID userId);
}
