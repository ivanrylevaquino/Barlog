package com.barlog.barlog_back.video;

import com.barlog.barlog_back.workout.ExerciseSet;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Video of one exercise set plus its pose-analysis result (FR-014, FR-018..FR-020). */
@Entity
@Table(name = "videos")
public class VideoClip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "video_id", updatable = false, nullable = false)
    private UUID videoId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false, unique = true)
    private ExerciseSet set;

    /** File name inside the storage directory. */
    @Column(name = "storage_key", nullable = false, length = 200)
    private String storageKey;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VideoStatus status = VideoStatus.UPLOADED;

    @Column(name = "failure_reason", length = 40)
    private String failureReason;

    /** Full JSON result returned by the analysis service. */
    @Column(name = "analysis_json", columnDefinition = "TEXT")
    private String analysisJson;

    @Column(name = "reps_detected")
    private Integer repsDetected;

    @Column(name = "hold_seconds")
    private Double holdSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getVideoId() { return videoId; }
    public ExerciseSet getSet() { return set; }
    public void setSet(ExerciseSet set) { this.set = set; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public VideoStatus getStatus() { return status; }
    public void setStatus(VideoStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getAnalysisJson() { return analysisJson; }
    public void setAnalysisJson(String analysisJson) { this.analysisJson = analysisJson; }
    public Integer getRepsDetected() { return repsDetected; }
    public void setRepsDetected(Integer repsDetected) { this.repsDetected = repsDetected; }
    public Double getHoldSeconds() { return holdSeconds; }
    public void setHoldSeconds(Double holdSeconds) { this.holdSeconds = holdSeconds; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(Instant analyzedAt) { this.analyzedAt = analyzedAt; }
}
