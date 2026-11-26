package com.barlog.barlog_back.workout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the predefined exercise catalogue (FR-006 / SRS Appendix A) on start-up.
 * Idempotent: an exercise type that already exists is left untouched.
 */
@Component
public class ExerciseCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExerciseCatalogSeeder.class);

    private final ExerciseRepository exerciseRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public ExerciseCatalogSeeder(ExerciseRepository exerciseRepository, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.exerciseRepository = exerciseRepository;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Hibernate adds a CHECK constraint listing the enum values when it first creates the table and never
        // updates it, so new exercise types would be rejected on an existing database. Drop it if present.
        try {
            jdbc.execute("ALTER TABLE exercises DROP CONSTRAINT IF EXISTS exercises_type_check");
        } catch (Exception e) {
            log.debug("No exercises_type_check constraint to drop: {}", e.getMessage());
        }
        seed("Push-up", ExerciseType.PUSH_UP, "Push", List.of("Incline", "Standard", "Diamond", "Archer"));
        seed("Pull-up", ExerciseType.PULL_UP, "Pull", List.of("Negative", "Assisted", "Strict", "Weighted"));
        seed("Squat", ExerciseType.SQUAT, "Legs", List.of("Assisted", "Bodyweight", "Split squat", "Pistol"));
        seed("Parallel bar dip", ExerciseType.DIP, "Push", List.of("Bench", "Assisted", "Parallel bar", "Weighted"));
        seedHold("Plank", ExerciseType.PLANK, "Core", List.of("Knee", "Forearm", "High", "Weighted"));
        seedHold("Dead hang", ExerciseType.DEAD_HANG, "Pull", List.of("Two-arm", "Active", "One-arm"));
        seedHold("L-sit", ExerciseType.L_SIT, "Core", List.of("Tuck", "One-leg", "Full"));
    }

    private void seedHold(String name, ExerciseType type, String category, List<String> progressions) {
        if (exerciseRepository.findByType(type).isEmpty()) {
            Exercise e = new Exercise(name, type, category, true, progressions);
            e.setHold(true);
            exerciseRepository.save(e);
            log.info("Seeded exercise catalogue entry: {}", name);
        }
    }

    private void seed(String name, ExerciseType type, String category, List<String> progressions) {
        if (exerciseRepository.findByType(type).isEmpty()) {
            exerciseRepository.save(new Exercise(name, type, category, true, progressions));
            log.info("Seeded exercise catalogue entry: {}", name);
        }
    }
}
