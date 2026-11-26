package com.barlog.barlog_back.journal;

import com.barlog.barlog_back.journal.JournalDtos.*;
import com.barlog.barlog_back.shared.BusinessRuleException;
import com.barlog.barlog_back.shared.ResourceNotFoundException;
import com.barlog.barlog_back.workout.ExerciseSet;
import com.barlog.barlog_back.workout.WorkoutSession;
import com.barlog.barlog_back.workout.WorkoutSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Post-session journal: "How do you feel after this session?" */
@Service
public class JournalService {

    private final JournalEntryRepository journalRepository;
    private final WorkoutSessionRepository sessionRepository;

    public JournalService(JournalEntryRepository journalRepository, WorkoutSessionRepository sessionRepository) {
        this.journalRepository = journalRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public List<JournalDto> list(UUID userId) {
        return journalRepository.findBySession_User_UserIdOrderBySession_StartedAtDesc(userId)
                .stream().map(JournalService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<JournalDto> get(UUID userId, UUID sessionId) {
        ownedSession(userId, sessionId);
        return journalRepository.findBySession_SessionId(sessionId).map(JournalService::toDto);
    }

    @Transactional
    public JournalDto save(UUID userId, UUID sessionId, JournalRequest req) {
        WorkoutSession s = ownedSession(userId, sessionId);
        if (!s.isFinished()) {
            throw new BusinessRuleException("Finish the workout before writing about it.");
        }
        JournalEntry e = journalRepository.findBySession_SessionId(sessionId).orElseGet(() -> {
            JournalEntry n = new JournalEntry();
            n.setSession(s);
            return n;
        });
        e.setMood(req.mood());
        e.setEnergy(req.energy());
        e.setTagList(req.tags());
        String body = req.body() == null ? null : req.body().strip();
        e.setBody(body == null || body.isEmpty() ? null : body);
        return toDto(journalRepository.saveAndFlush(e));
    }

    @Transactional
    public void delete(UUID userId, UUID sessionId) {
        ownedSession(userId, sessionId);
        JournalEntry e = journalRepository.findBySession_SessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No journal entry for this workout."));
        journalRepository.delete(e);
    }

    private WorkoutSession ownedSession(UUID userId, UUID sessionId) {
        return sessionRepository.findBySessionIdAndUser_UserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found."));
    }

    static JournalDto toDto(JournalEntry e) {
        WorkoutSession s = e.getSession();
        List<ExerciseSet> sets = s.getSets();
        Recap recap = new Recap(
                sets.stream().map(x -> x.getExercise().getName()).distinct().toList(),
                sets.stream().mapToInt(ExerciseSet::getSetsCount).sum(),
                sets.stream().mapToInt(x -> x.getReps() * x.getSetsCount()).sum());
        return new JournalDto(e.getEntryId(), s.getSessionId(), s.getStartedAt(), e.getMood(), e.getEnergy(),
                e.getTagList(), e.getBody() == null ? "" : e.getBody(), e.getCreatedAt(), e.getUpdatedAt(), recap);
    }
}
