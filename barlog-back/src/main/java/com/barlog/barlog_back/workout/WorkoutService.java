package com.barlog.barlog_back.workout;

import com.barlog.barlog_back.auth.User;
import com.barlog.barlog_back.auth.UserRepository;
import com.barlog.barlog_back.journal.JournalEntry;
import com.barlog.barlog_back.journal.JournalEntryRepository;
import com.barlog.barlog_back.journal.Mood;
import com.barlog.barlog_back.shared.BusinessRuleException;
import com.barlog.barlog_back.shared.ResourceNotFoundException;
import com.barlog.barlog_back.workout.WorkoutDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Business logic for workout sessions, sets, history and progress (FR-005..FR-009, FR-021..FR-023, FR-025).
 * Every lookup is scoped to the authenticated user, so one practitioner can never read or change
 * another practitioner's data: a foreign id simply returns 404.
 */
@Service
public class WorkoutService {

    private static final Set<String> RANGES = Set.of("7", "30", "90", "all");

    private final ExerciseRepository exerciseRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final ExerciseSetRepository setRepository;
    private final UserRepository userRepository;
    private final JournalEntryRepository journalRepository;
    private final ZoneId zone;

    public WorkoutService(ExerciseRepository exerciseRepository, WorkoutSessionRepository sessionRepository,
                          ExerciseSetRepository setRepository, UserRepository userRepository,
                          JournalEntryRepository journalRepository) {
        this.exerciseRepository = exerciseRepository;
        this.sessionRepository = sessionRepository;
        this.setRepository = setRepository;
        this.userRepository = userRepository;
        this.journalRepository = journalRepository;
        this.zone = ZoneId.systemDefault();
    }

    // ── Exercise catalogue (FR-006) ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExerciseDto> listExercises() {
        return exerciseRepository.findAllByOrderByExerciseIdAsc().stream().map(ExerciseDto::from).toList();
    }

    // ── Sessions (FR-005, FR-025) ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SessionDto> listSessions(UUID userId) {
        Map<UUID, Mood> moods = new HashMap<>();
        for (JournalEntry j : journalRepository.findBySession_User_UserIdOrderBySession_StartedAtDesc(userId)) {
            moods.put(j.getSession().getSessionId(), j.getMood());
        }
        return sessionRepository.findByUser_UserIdOrderByStartedAtDesc(userId).stream()
                .map(s -> SessionDto.from(s, moods.get(s.getSessionId()))).toList();
    }

    @Transactional(readOnly = true)
    public Optional<SessionDto> getActiveSession(UUID userId) {
        return sessionRepository.findFirstByUser_UserIdAndEndedAtIsNullOrderByStartedAtDesc(userId).map(SessionDto::from);
    }

    @Transactional(readOnly = true)
    public SessionDto getSession(UUID userId, UUID sessionId) {
        return SessionDto.from(ownedSession(userId, sessionId),
                journalRepository.findBySession_SessionId(sessionId).map(JournalEntry::getMood).orElse(null));
    }

    @Transactional
    public SessionDto createSession(UUID userId, SessionRequest req) {
        if (sessionRepository.existsByUser_UserIdAndEndedAtIsNull(userId)) {
            throw new BusinessRuleException("You already have a workout in progress. Finish or discard it before starting a new one.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        WorkoutSession s = new WorkoutSession();
        s.setUser(user);
        s.setStartedAt(Instant.now());
        s.setNotes(clean(req == null ? null : req.notes()));
        return SessionDto.from(sessionRepository.save(s));
    }

    @Transactional
    public SessionDto updateSession(UUID userId, UUID sessionId, SessionRequest req) {
        WorkoutSession s = ownedSession(userId, sessionId);
        s.setNotes(clean(req == null ? null : req.notes()));
        return SessionDto.from(s);
    }

    @Transactional
    public SessionDto finishSession(UUID userId, UUID sessionId) {
        WorkoutSession s = ownedSession(userId, sessionId);
        if (s.isFinished()) throw new BusinessRuleException("This workout has already been finished.");
        if (s.getSets().isEmpty()) throw new BusinessRuleException("Log at least one set before finishing the workout.");
        s.setEndedAt(Instant.now());
        return SessionDto.from(s);
    }

    @Transactional
    public void deleteSession(UUID userId, UUID sessionId) {
        WorkoutSession s = ownedSession(userId, sessionId);
        journalRepository.findBySession_SessionId(sessionId).ifPresent(journalRepository::delete);
        sessionRepository.delete(s); // sets cascade (orphanRemoval)
    }

    // ── Sets (FR-007, FR-008, FR-009) ─────────────────────────────────────

    @Transactional
    public SetDto addSet(UUID userId, UUID sessionId, SetRequest req) {
        WorkoutSession s = ownedSession(userId, sessionId);
        if (s.isFinished()) throw new BusinessRuleException("This workout is finished. Start a new workout to log more sets.");
        Exercise ex = exerciseRepository.findById(req.exerciseId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercise not found."));

        ExerciseSet set = new ExerciseSet();
        set.setSession(s);
        set.setExercise(ex);
        set.setSetNumber((int) s.getSets().stream()
                .filter(x -> x.getExercise().getExerciseId().equals(ex.getExerciseId())).count() + 1);
        apply(set, ex, req);
        s.getSets().add(set);
        return SetDto.from(setRepository.save(set));
    }

    @Transactional(readOnly = true)
    public SetDto getSet(UUID userId, UUID setId) {
        return SetDto.from(ownedSet(userId, setId));
    }

    @Transactional(readOnly = true)
    public List<SetDto> listSets(UUID userId, boolean hasVideo) {
        // Video upload (FR-010..FR-014) is not built yet, so no set has a video.
        if (hasVideo) return List.of();
        return setRepository.findBySession_User_UserIdOrderByCreatedAtDesc(userId).stream().map(SetDto::from).toList();
    }

    @Transactional
    public SetDto updateSet(UUID userId, UUID setId, SetRequest req) {
        ExerciseSet set = ownedSet(userId, setId);
        if (!set.getExercise().getExerciseId().equals(req.exerciseId())) {
            throw new IllegalArgumentException("The exercise of a logged set cannot be changed. Delete the set and log it again.");
        }
        apply(set, set.getExercise(), req);
        return SetDto.from(set);
    }

    @Transactional
    public void deleteSet(UUID userId, UUID setId) {
        ExerciseSet set = ownedSet(userId, setId);
        WorkoutSession s = set.getSession();
        Long exId = set.getExercise().getExerciseId();
        s.getSets().remove(set); // orphanRemoval deletes the row
        int n = 1;
        for (ExerciseSet x : s.getSets()) {
            if (x.getExercise().getExerciseId().equals(exId)) x.setSetNumber(n++);
        }
    }

    // ── Progress analytics (FR-021, FR-022, FR-023) ───────────────────────

    @Transactional(readOnly = true)
    public ProgressDto getProgress(UUID userId, String range, Long exerciseId) {
        String r = (range == null || range.isBlank()) ? "30" : range.trim().toLowerCase();
        if (!RANGES.contains(r)) throw new IllegalArgumentException("Range must be one of 7, 30, 90 or all.");
        if (exerciseId != null && !exerciseRepository.existsById(exerciseId)) {
            throw new ResourceNotFoundException("Exercise not found.");
        }
        Instant from = r.equals("all") ? Instant.EPOCH : Instant.now().minus(Long.parseLong(r), ChronoUnit.DAYS);

        record Day(int[] volMax, Set<UUID> sessions) {}
        TreeMap<LocalDate, Day> byDay = new TreeMap<>();
        Set<UUID> allSessions = new HashSet<>();
        int volume = 0, maxReps = 0;

        for (WorkoutSession s : sessionRepository.findByUser_UserIdAndStartedAtGreaterThanEqualOrderByStartedAtAsc(userId, from)) {
            LocalDate date = s.getStartedAt().atZone(zone).toLocalDate();
            for (ExerciseSet set : s.getSets()) {
                if (exerciseId != null && !set.getExercise().getExerciseId().equals(exerciseId)) continue;
                Day d = byDay.computeIfAbsent(date, k -> new Day(new int[2], new HashSet<>()));
                d.volMax()[0] += set.getReps();
                d.volMax()[1] = Math.max(d.volMax()[1], set.getReps());
                d.sessions().add(s.getSessionId());
                allSessions.add(s.getSessionId());
                volume += set.getReps();
                maxReps = Math.max(maxReps, set.getReps());
            }
        }
        List<ProgressDto.Point> points = byDay.entrySet().stream()
                .map(e -> new ProgressDto.Point(e.getKey(), e.getValue().volMax()[0], e.getValue().volMax()[1], e.getValue().sessions().size()))
                .toList();
        return new ProgressDto(new ProgressDto.Totals(volume, maxReps, allSessions.size()), points);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private WorkoutSession ownedSession(UUID userId, UUID sessionId) {
        return sessionRepository.findBySessionIdAndUser_UserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Workout session not found."));
    }

    private ExerciseSet ownedSet(UUID userId, UUID setId) {
        return setRepository.findBySetIdAndSession_User_UserId(setId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Set not found."));
    }

    private void apply(ExerciseSet set, Exercise ex, SetRequest req) {
        String stage = ex.canonicalProgression(req.progression());
        if (stage == null) {
            throw new IllegalArgumentException("'" + req.progression() + "' is not a progression stage for " + ex.getName()
                    + ". Choose one of: " + String.join(", ", ex.getProgressionList()) + ".");
        }
        set.setProgression(stage);
        set.setReps(req.reps());
        set.setWeightKg(req.weightKg() == null ? BigDecimal.ZERO : req.weightKg().setScale(2, RoundingMode.HALF_UP));
        set.setRpe(req.rpe());
        set.setNotes(clean(req.notes()));
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
