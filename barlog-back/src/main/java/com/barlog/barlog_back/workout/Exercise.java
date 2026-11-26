package com.barlog.barlog_back.workout;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Predefined calisthenics exercise (FR-006). Rows are seeded by {@link ExerciseCatalogSeeder};
 * practitioners select from this catalogue but cannot create or modify entries.
 */
@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_id")
    private Long exerciseId;

    @Column(name = "name", nullable = false, unique = true, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, unique = true, length = 20)
    private ExerciseType type;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "analysis_supported", nullable = false)
    private boolean analysisSupported;

    /** True for timed holds (plank, dead hang, L-sit): logged in seconds instead of reps. */
    @Column(name = "is_hold")
    private Boolean hold;

    /** Ordered progression stages, easiest first, stored comma-separated (FR-008). */
    @Column(name = "progressions", nullable = false, length = 500)
    private String progressions;

    public Exercise() {}

    public Exercise(String name, ExerciseType type, String category, boolean analysisSupported, List<String> progressions) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.analysisSupported = analysisSupported;
        setProgressionList(progressions);
    }

    public List<String> getProgressionList() {
        if (progressions == null || progressions.isBlank()) return new ArrayList<>();
        return Arrays.stream(progressions.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public void setProgressionList(List<String> list) {
        this.progressions = String.join(",", list);
    }

    /** Returns the catalogue spelling of the given stage, or null if it is not a valid stage for this exercise. */
    public String canonicalProgression(String stage) {
        if (stage == null) return null;
        String s = stage.trim();
        return getProgressionList().stream().filter(p -> p.equalsIgnoreCase(s)).findFirst().orElse(null);
    }

    public Long getExerciseId() { return exerciseId; }
    public void setExerciseId(Long exerciseId) { this.exerciseId = exerciseId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ExerciseType getType() { return type; }
    public void setType(ExerciseType type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isAnalysisSupported() { return analysisSupported; }
    public boolean isHold() { return Boolean.TRUE.equals(hold); }
    public void setHold(boolean hold) { this.hold = hold; }
    public void setAnalysisSupported(boolean analysisSupported) { this.analysisSupported = analysisSupported; }
    public String getProgressions() { return progressions; }
    public void setProgressions(String progressions) { this.progressions = progressions; }
}
