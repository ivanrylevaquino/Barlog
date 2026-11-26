package com.barlog.barlog_back.workout;

import com.barlog.barlog_back.security.UserPrincipal;
import com.barlog.barlog_back.workout.WorkoutDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Workout logging REST API. All endpoints require ROLE_PRACTITIONER and operate on the caller's own data. */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('PRACTITIONER')")
public class WorkoutController {

    private final WorkoutService service;

    public WorkoutController(WorkoutService service) {
        this.service = service;
    }

    // FR-006
    @GetMapping("/exercises")
    public List<ExerciseDto> exercises() {
        return service.listExercises();
    }

    // FR-005, FR-025
    @GetMapping("/sessions")
    public List<SessionDto> sessions(@AuthenticationPrincipal UserPrincipal me) {
        return service.listSessions(me.getUserId());
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<SessionDto> activeSession(@AuthenticationPrincipal UserPrincipal me) {
        return service.getActiveSession(me.getUserId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionDto> createSession(@AuthenticationPrincipal UserPrincipal me,
                                                    @Valid @RequestBody(required = false) SessionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSession(me.getUserId(), req));
    }

    @GetMapping("/sessions/{id}")
    public SessionDto session(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id) {
        return service.getSession(me.getUserId(), id);
    }

    @PatchMapping("/sessions/{id}")
    public SessionDto updateSession(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id,
                                    @Valid @RequestBody SessionRequest req) {
        return service.updateSession(me.getUserId(), id, req);
    }

    @PatchMapping("/sessions/{id}/finish")
    public SessionDto finishSession(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id) {
        return service.finishSession(me.getUserId(), id);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id) {
        service.deleteSession(me.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    // FR-007, FR-008, FR-009
    @PostMapping("/sessions/{id}/sets")
    public ResponseEntity<SetDto> addSet(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id,
                                         @Valid @RequestBody SetRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addSet(me.getUserId(), id, req));
    }

    @GetMapping("/sets")
    public List<SetDto> sets(@AuthenticationPrincipal UserPrincipal me,
                             @RequestParam(defaultValue = "false") boolean hasVideo) {
        return service.listSets(me.getUserId(), hasVideo);
    }

    @GetMapping("/sets/{id}")
    public SetDto set(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id) {
        return service.getSet(me.getUserId(), id);
    }

    @PatchMapping("/sets/{id}")
    public SetDto updateSet(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id,
                            @Valid @RequestBody SetRequest req) {
        return service.updateSet(me.getUserId(), id, req);
    }

    @DeleteMapping("/sets/{id}")
    public ResponseEntity<Void> deleteSet(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id) {
        service.deleteSet(me.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    // FR-021, FR-022, FR-023
    @GetMapping("/progress")
    public ProgressDto progress(@AuthenticationPrincipal UserPrincipal me,
                                @RequestParam(required = false) String range,
                                @RequestParam(required = false) Long exerciseId) {
        return service.getProgress(me.getUserId(), range, exerciseId);
    }
}
