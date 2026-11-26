package com.barlog.barlog_back.workout;

import com.barlog.barlog_back.journal.Mood;
import com.barlog.barlog_back.video.VideoService;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

            @Min(value = 0, message = "Reps cannot be negative.")
            @Max(value = 500, message = "Reps cannot exceed 500.")
            Integer reps,

            @Min(value = 1, message = "Sets must be at least 1.")
            @Max(value = 50, message = "Sets cannot exceed 50.")
            Integer sets,

            @Min(value = 1, message = "Hold time must be at least 1 second.")
            @Max(value = 3600, message = "Hold time cannot exceed an hour.")
            Integer holdSeconds,

            /** Reps (or seconds, for holds) of each set. When given, it decides the number of sets. */
            @Size(max = 50, message = "Sets cannot exceed 50.")
            List<Integer> perSet,

            @DecimalMin(value = "0.0", message = "Added weight cannot be negative.")
            @DecimalMax(value = "300.0", message = "Added weight cannot exceed 300 kg.")
            BigDecimal weightKg,

            @Min(value = 1, message = "RPE must be between 1 and 10.")
            @Max(value = 10, message = "RPE must be between 1 and 10.")
            Integer rpe,

            @Size(max = 500, message = "Notes must be 500 characters or fewer.")
            String notes) {}

    // ── Responses ─────────────────────────────────────────────────────────

    public record ExerciseDto(Long id, String name, ExerciseType type, String category,
                              boolean analysisSupported, List<String> progressions, boolean hold) {
        static ExerciseDto from(Exercise e) {
            return new ExerciseDto(e.getExerciseId(), e.getName(), e.getType(), e.getCategory(),
                    e.isAnalysisSupported(), e.getProgressionList(), e.isHold());
        }
    }

    /** {@code video} is null when no video is attached to the set. */
    public record SetDto(UUID id, UUID sessionId, Instant sessionDate, Long exerciseId, String exerciseName,
                         int setNumber, int reps, String progression, BigDecimal weightKg, Integer rpe,
                         String notes, Instant createdAt, VideoService.VideoDto video,
                         int sets, Integer holdSeconds, ExerciseType exerciseType, boolean hold, List<Integer> perSet) {
        static SetDto from(ExerciseSet s) {
            return from(s, null);
        }

        static SetDto from(ExerciseSet s, VideoService.VideoDto video) {
            WorkoutSession session = s.getSession();
            Exercise ex = s.getExercise();
            return new SetDto(s.getSetId(), session.getSessionId(), session.getStartedAt(), ex.getExerciseId(),
                    ex.getName(), s.getSetNumber(), s.getReps(), s.getProgression(), s.getWeightKg(), s.getRpe(),
                    s.getNotes() == null ? "" : s.getNotes(), s.getCreatedAt(), video,
                    s.getSetsCount(), s.getHoldSeconds(), ex.getType(), ex.isHold(), s.getPerSetList());
        }
    }

    /** {@code mood} is the post-session journal mood, or null when no entry has been written. */
    public record SessionDto(UUID id, Instant date, Instant startedAt, Instant endedAt, String notes,
                             List<SetDto> sets, Mood mood) {
        static SessionDto from(WorkoutSession s) {
            return from(s, null, Map.of());
        }

        static SessionDto from(WorkoutSession s, Mood mood, Map<UUID, VideoService.VideoDto> videos) {
            return new SessionDto(s.getSessionId(), s.getStartedAt(), s.getStartedAt(), s.getEndedAt(),
                    s.getNotes() == null ? "" : s.getNotes(),
                    s.getSets().stream().map(x -> SetDto.from(x, videos.get(x.getSetId()))).toList(), mood);
        }
    }

    /** GET /api/progress (FR-021, FR-022, FR-023). */
    public record ProgressDto(Totals totals, List<Point> points) {
        public record Totals(int volume, int maxReps, int sessions) {}
        public record Point(LocalDate date, int volume, int maxReps, int sessions) {}
    }
}
