package com.barlog.barlog_back.journal;

import com.barlog.barlog_back.journal.JournalDtos.*;
import com.barlog.barlog_back.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('PRACTITIONER')")
public class JournalController {

    private final JournalService service;

    public JournalController(JournalService service) {
        this.service = service;
    }

    @GetMapping("/journal")
    public List<JournalDto> list(@AuthenticationPrincipal UserPrincipal me) {
        return service.list(me.getUserId());
    }

    @GetMapping("/sessions/{id}/journal")
    public ResponseEntity<JournalDto> get(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id) {
        return service.get(me.getUserId(), id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/sessions/{id}/journal")
    public JournalDto save(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id,
                           @Valid @RequestBody JournalRequest req) {
        return service.save(me.getUserId(), id, req);
    }

    @DeleteMapping("/sessions/{id}/journal")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable UUID id) {
        service.delete(me.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
