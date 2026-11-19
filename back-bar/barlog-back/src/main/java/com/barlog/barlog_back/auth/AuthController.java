package com.barlog.barlog_back.auth;

import com.barlog.barlog_back.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/google — exchange Google ID token for a BarLog JWT. */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateGoogle(@Valid @RequestBody GoogleAuthRequest req) {
        return ResponseEntity.ok(authService.authenticateGoogle(req.getIdToken()));
    }

    /** GET /api/auth/me — return the calling user's profile. */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getUserId()));
    }

    /** PATCH /api/auth/me/preferences — toggle preferred weight unit (KG / LB). */
    @PatchMapping("/me/preferences")
    public ResponseEntity<UserDto> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePreferencesRequest req) {
        return ResponseEntity.ok(authService.updatePreferences(principal.getUserId(), req.getPreferredUnit()));
    }

    /** POST /api/auth/logout — invalidate the JWT server-side. */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(Collections.singletonMap("message", "Logged out successfully"));
    }
}
