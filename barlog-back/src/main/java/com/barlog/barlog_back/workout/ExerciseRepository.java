package com.barlog.barlog_back.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Optional<Exercise> findByType(ExerciseType type);
    List<Exercise> findAllByOrderByExerciseIdAsc();
}
