package com.barlog.barlog_back.journal;

import com.barlog.barlog_back.workout.WorkoutSession;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** A post-workout reflection: one journal entry per finished session. */
@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entry_id", updatable = false, nullable = false)
    private UUID entryId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private WorkoutSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "mood", nullable = false, length = 20)
    private Mood mood;

    /** 1 (empty tank) to 5 (could go again); optional. */
    @Column(name = "energy")
    private Integer energy;

    /** Comma-separated {@link JournalTag} names. */
    @Column(name = "tags", length = 300)
    private String tags;

    @Column(name = "body", length = 5000)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public List<JournalTag> getTagList() {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split(",")).map(JournalTag::valueOf).toList();
    }

    public void setTagList(List<JournalTag> list) {
        this.tags = list == null || list.isEmpty() ? null
                : String.join(",", list.stream().distinct().map(Enum::name).toList());
    }

    public UUID getEntryId() { return entryId; }
    public WorkoutSession getSession() { return session; }
    public void setSession(WorkoutSession session) { this.session = session; }
    public Mood getMood() { return mood; }
    public void setMood(Mood mood) { this.mood = mood; }
    public Integer getEnergy() { return energy; }
    public void setEnergy(Integer energy) { this.energy = energy; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
