package com.barlog.barlog_back.workout;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Request and response bodies for the workout logging API. Field names match barlog-web/src/api/index.js. */
public final class WorkoutDtos {

    private WorkoutDtos() {}

    // ── Requests ──────────────────────────────────────────────────────────

    /** POST /api/sessions and PATCH /api/sessions/{id} (FR-005). */
    public record SessionRequest(
            @Size(max = 1000, message = "Session notes must be 1000 characters or fewer.")
            String notes) {}

    /** POST /api/sessions/{id}/sets and PATCH /api/sets/{id} (FR-007, FR-008, FR-009). */
    public record SetRequest(
            @NotNull(message = "Choose an exercise.")
            Long exerciseId,

            @NotBlank(message = "Choose a progression stage.")
            @Size(max = 60, message = "Progression must be 60 characters or fewer.")
            String progression,

            @NotNull(message = "Enter the number of reps.")
            @Min(value = 1, message = "Reps must be at least 1.")
            @Max(value = 500, message = "Reps cannot exceed 500.")
            Integer reps,

            @DecimalMin(value = "0.0", message = "Added weight cannot be negative.")
            @DecimalMax(value = "300.0", message = "Added weight cannot exceed 300 kg.")
            BigDecimal weightKg,

            @Min(value = 1, message = "RPE must be between 1 and 10.")
            @Max(value = 10, message = "RPE must be between 1 and 10.")
            Integer rpe,

            @Size(max = 500, message = "Set notes must be 500 characters or fewer.")
            String notes) {}

    // ── Responses ─────────────────────────────────────────────────────────

    public record ExerciseDto(Long id, String name, ExerciseType type, String category,
                              boolean analysisSupported, List<String> progressions) {
        static ExerciseDto from(Exercise e) {
            return new ExerciseDto(e.getExerciseId(), e.getName(), e.getType(), e.getCategory(),
                    e.isAnalysisSupported(), e.getProgressionList());
        }
    }

    /** {@code video} is always null until the video upload module (FR-010..FR-014) is implemented. */
    public record SetDto(UUID id, UUID sessionId, Instant sessionDate, Long exerciseId, String exerciseName,
                         int setNumber, int reps, String progression, BigDecimal weightKg, Integer rpe,
                         String notes, Instant createdAt, Object video) {
        static SetDto from(ExerciseSet s) {
            WorkoutSession session = s.getSession();
            Exercise ex = s.getExercise();
            return new SetDto(s.getSetId(), session.getSessionId(), session.getStartedAt(), ex.getExerciseId(),
                    ex.getName(), s.getSetNumber(), s.getReps(), s.getProgression(), s.getWeightKg(), s.getRpe(),
                    s.getNotes() == null ? "" : s.getNotes(), s.getCreatedAt(), null);
        }
    }

    public record SessionDto(UUID id, Instant date, Instant startedAt, Instant endedAt, String notes, List<SetDto> sets) {
        static SessionDto from(WorkoutSession s) {
            return new SessionDto(s.getSessionId(), s.getStartedAt(), s.getStartedAt(), s.getEndedAt(),
                    s.getNotes() == null ? "" : s.getNotes(),
                    s.getSets().stream().map(SetDto::from).toList());
        }
    }

    /** GET /api/progress (FR-021, FR-022, FR-023). */
    public record ProgressDto(Totals totals, List<Point> points) {
        public record Totals(int volume, int maxReps, int sessions) {}
        public record Point(LocalDate date, int volume, int maxReps, int sessions) {}
    }
}
