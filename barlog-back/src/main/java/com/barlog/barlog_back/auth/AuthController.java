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



    /** POST /api/auth/login — password based login. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody PasswordLoginRequest req) {
        return ResponseEntity.ok(authService.loginWithPassword(req));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateGoogle(@Valid @RequestBody GoogleAuthRequest req) {
        return ResponseEntity.ok(authService.authenticateGoogle(req.getIdToken()));
    }

    /** GET /api/auth/me — return the calling user's profile. */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getUserId()));
    }

    /** PATCH /api/auth/me — update display name or avatar URL. */
    @PatchMapping("/me")
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(authService.updateProfile(principal.getUserId(), req.getDisplayName(), req.getAvatarUrl()));
    }

    /** PATCH /api/auth/me/password — set or change password. */
    @PatchMapping("/me/password")
    public ResponseEntity<AuthResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest req) {
        return ResponseEntity.ok(authService.changePassword(principal.getUserId(), req.getCurrentPassword(), req.getNewPassword()));
    }

    /** POST /api/auth/forgot-password — request a password reset token. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody PasswordResetRequest req) {
        authService.requestPasswordReset(req);
        return ResponseEntity.ok(Collections.singletonMap("message", "If that email exists, a reset link has been sent."));
    }

    /** POST /api/auth/reset-password — apply a password reset token. */
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(authService.resetPassword(req.getToken(), req.getNewPassword()));
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

