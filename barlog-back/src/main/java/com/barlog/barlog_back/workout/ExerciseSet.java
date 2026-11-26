package com.barlog.barlog_back.workout;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One logged set inside a workout session (FR-007, FR-008, FR-009). */
@Entity
@Table(name = "exercise_sets")
public class ExerciseSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "set_id", updatable = false, nullable = false)
    private UUID setId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private WorkoutSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "reps", nullable = false)
    private int reps;

    @Column(name = "progression", nullable = false, length = 60)
    private String progression;

    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg = BigDecimal.ZERO;

    @Column(name = "rpe")
    private Integer rpe;

    /** How many sets this entry represents (one entry per exercise in a workout). Null on old rows = 1. */
    @Column(name = "sets_count")
    private Integer setsCount;

    /** Seconds held per set, for hold exercises. */
    @Column(name = "hold_seconds")
    private Integer holdSeconds;

    /** Reps (or seconds, for holds) of each set, comma-separated, e.g. "10,8,6". Null on old rows. */
    @Column(name = "per_set", length = 400)
    private String perSet;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ExerciseSet() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (weightKg == null) weightKg = BigDecimal.ZERO;
    }

    public UUID getSetId() { return setId; }
    public void setSetId(UUID setId) { this.setId = setId; }
    public WorkoutSession getSession() { return session; }
    public void setSession(WorkoutSession session) { this.session = session; }
    public Exercise getExercise() { return exercise; }
    public void setExercise(Exercise exercise) { this.exercise = exercise; }
    public int getSetNumber() { return setNumber; }
    public void setSetNumber(int setNumber) { this.setNumber = setNumber; }
    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }
    public String getProgression() { return progression; }
    public void setProgression(String progression) { this.progression = progression; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public Integer getRpe() { return rpe; }
    public int getSetsCount() { return setsCount == null ? 1 : setsCount; }
    public void setSetsCount(Integer setsCount) { this.setsCount = setsCount; }
    public Integer getHoldSeconds() { return holdSeconds; }
    public void setHoldSeconds(Integer holdSeconds) { this.holdSeconds = holdSeconds; }

    /** Reps (or seconds) of each set; old rows fall back to the same value for every set. */
    public java.util.List<Integer> getPerSetList() {
        if (perSet != null && !perSet.isBlank()) {
            return java.util.Arrays.stream(perSet.split(",")).map(String::trim).map(Integer::valueOf).toList();
        }
        int each = holdSeconds != null ? holdSeconds : reps;
        return java.util.Collections.nCopies(getSetsCount(), each);
    }

    public void setPerSetList(java.util.List<Integer> values) {
        this.perSet = values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    /** Total reps across all sets (0 for holds). */
    public int getTotalReps() {
        return holdSeconds != null ? 0 : getPerSetList().stream().mapToInt(Integer::intValue).sum();
    }
    public void setRpe(Integer rpe) { this.rpe = rpe; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
