package com.barlog.barlog_back.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistent user record created on first Google login (FR-002).
 * Stores only the minimum data required by SRS NFR-017.
 * No password field: auth is passwordless per FR-001 / NFR-004.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "google_id", unique = true, nullable = false)
    private String googleId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.PRACTITIONER;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_unit", nullable = false)
    private PreferredUnit preferredUnit = PreferredUnit.KG;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_hash")
    private String passwordHash;   // BCrypt hash; may be null for Google‑only accounts

    public User() {}

    public User(UUID userId, String googleId, String email, String displayName,
                String avatarUrl, Role role, PreferredUnit preferredUnit,
                Instant createdAt, Instant lastLoginAt) {
        this.userId = userId;
        this.googleId = googleId;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.role = role != null ? role : Role.PRACTITIONER;
        this.preferredUnit = preferredUnit != null ? preferredUnit : PreferredUnit.KG;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.lastLoginAt = lastLoginAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (role == null) role = Role.PRACTITIONER;
        if (preferredUnit == null) preferredUnit = PreferredUnit.KG;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public PreferredUnit getPreferredUnit() { return preferredUnit; }
    public void setPreferredUnit(PreferredUnit preferredUnit) { this.preferredUnit = preferredUnit; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    // ── Builder ────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID userId;
        private String googleId;
        private String email;
        private String displayName;
        private String avatarUrl;
        private Role role = Role.PRACTITIONER;
        private PreferredUnit preferredUnit = PreferredUnit.KG;
        private Instant createdAt;
        private Instant lastLoginAt;

    @Column(name = "password_hash")
    private String passwordHash;   // BCrypt hash; may be null for Google‑only accounts

        public Builder userId(UUID v) { userId = v; return this; }
        public Builder googleId(String v) { googleId = v; return this; }
        public Builder email(String v) { email = v; return this; }
        public Builder displayName(String v) { displayName = v; return this; }
        public Builder avatarUrl(String v) { avatarUrl = v; return this; }
        public Builder role(Role v) { role = v; return this; }
        public Builder preferredUnit(PreferredUnit v) { preferredUnit = v; return this; }
        public Builder createdAt(Instant v) { createdAt = v; return this; }
        public Builder lastLoginAt(Instant v) { lastLoginAt = v; return this; }

        public User build() {
            return new User(userId, googleId, email, displayName, avatarUrl,
                            role, preferredUnit, createdAt, lastLoginAt);
        }
    }
}

