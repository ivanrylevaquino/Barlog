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

    public ExerciseCatalogSeeder(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("Push-up", ExerciseType.PUSH_UP, "Push", List.of("Incline", "Standard", "Diamond", "Archer"));
        seed("Pull-up", ExerciseType.PULL_UP, "Pull", List.of("Negative", "Assisted", "Strict", "Weighted"));
        seed("Squat", ExerciseType.SQUAT, "Legs", List.of("Assisted", "Bodyweight", "Split squat", "Pistol"));
        seed("Parallel bar dip", ExerciseType.DIP, "Push", List.of("Bench", "Assisted", "Parallel bar", "Weighted"));
    }

    private void seed(String name, ExerciseType type, String category, List<String> progressions) {
        if (exerciseRepository.findByType(type).isEmpty()) {
            exerciseRepository.save(new Exercise(name, type, category, true, progressions));
            log.info("Seeded exercise catalogue entry: {}", name);
        }
    }
}
