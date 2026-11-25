package com.barlog.barlog_back.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Verifies a Google ID token against the configured GOOGLE_CLIENT_ID.
 * Returns the minimum set of claims needed per SRS NFR-017:
 * google_id, email, display_name, avatar_url.
 */
@Service
public class GoogleTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierService.class);

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(@Value("${barlog.google.client-id:}") String clientId) {
        GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance());
        String trimmed = clientId != null ? clientId.trim() : "";
        if (!trimmed.isEmpty()) builder.setAudience(Collections.singletonList(trimmed));
        this.verifier = builder.build();
    }

    public GoogleUserInfo verify(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new InvalidGoogleTokenException("Invalid or expired Google sign-in. Please try again.");
        }
        try {
            GoogleIdToken token = verifier.verify(idTokenString.trim());
            if (token == null) throw new InvalidGoogleTokenException(
                    "Invalid or expired Google sign-in. Please try again.");

            GoogleIdToken.Payload payload = token.getPayload();
            Boolean verified = payload.getEmailVerified();
            if (verified == null || !verified) throw new InvalidGoogleTokenException(
                    "Google account email is not verified.");

            String sub     = payload.getSubject();
            String email   = payload.getEmail();
            String name    = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            if (sub == null || sub.isBlank() || email == null || email.isBlank())
                throw new InvalidGoogleTokenException("Invalid or expired Google sign-in. Please try again.");

            return new GoogleUserInfo(sub, email, name, picture);
        } catch (InvalidGoogleTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification error: {}", e.getMessage(), e);
            throw new InvalidGoogleTokenException("Invalid or expired Google sign-in. Please try again.", e);
        }
    }

    // ── Nested value object ────────────────────────────────────────────────

    public static class GoogleUserInfo {
        private final String googleId;
        private final String email;
        private final String displayName;
        private final String avatarUrl;

        public GoogleUserInfo(String googleId, String email, String displayName, String avatarUrl) {
            this.googleId = googleId;
            this.email = email;
            this.displayName = (displayName != null && !displayName.isBlank()) ? displayName.trim() : email;
            this.avatarUrl = avatarUrl;
        }

        public String getGoogleId() { return googleId; }
        public String getEmail() { return email; }
        public String getDisplayName() { return displayName; }
        public String getAvatarUrl() { return avatarUrl; }
    }

    // ── Exception ──────────────────────────────────────────────────────────

    public static class InvalidGoogleTokenException extends RuntimeException {
        public InvalidGoogleTokenException(String message) { super(message); }
        public InvalidGoogleTokenException(String message, Throwable cause) { super(message, cause); }
    }
}
