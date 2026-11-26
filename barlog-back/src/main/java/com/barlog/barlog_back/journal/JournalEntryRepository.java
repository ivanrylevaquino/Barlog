package com.barlog.barlog_back.journal;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findBySession_SessionId(UUID sessionId);

    @EntityGraph(attributePaths = {"session", "session.sets", "session.sets.exercise"})
    List<JournalEntry> findBySession_User_UserIdOrderBySession_StartedAtDesc(UUID userId);
}
