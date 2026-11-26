package com.barlog.barlog_back.journal;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class JournalDtos {

    private JournalDtos() {}

    /** PUT /api/sessions/{id}/journal */
    public record JournalRequest(
            @NotNull(message = "Pick how you feel after this session.")
            Mood mood,

            @Min(value = 1, message = "Energy must be between 1 and 5.")
            @Max(value = 5, message = "Energy must be between 1 and 5.")
            Integer energy,

            @Size(max = 6, message = "Pick up to 6 tags.")
            List<JournalTag> tags,

            @Size(max = 5000, message = "Journal entries must be 5000 characters or fewer.")
            String body) {}

    /** Short summary of the workout the entry is about. */
    public record Recap(List<String> exercises, int sets, int reps) {}

    public record JournalDto(UUID id, UUID sessionId, Instant sessionDate, Mood mood, Integer energy,
                             List<JournalTag> tags, String body, Instant createdAt, Instant updatedAt, Recap recap) {}
}
