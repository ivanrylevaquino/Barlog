package com.barlog.barlog_back.auth;

import java.time.Instant;
import java.util.UUID;

/** Read-only view of a {@link User} exposed to API consumers. */
public class UserDto {
    private UUID userId;
    private String googleId;
    private String email;
    private String displayName;
    private String avatarUrl;
    private Role role;
    private PreferredUnit preferredUnit;
    private Instant createdAt;
    private Instant lastLoginAt;
    private boolean hasPassword;

    public UserDto() {}

    public UserDto(UUID userId, String googleId, String email, String displayName, String avatarUrl,
                   Role role, PreferredUnit preferredUnit, Instant createdAt, Instant lastLoginAt, boolean hasPassword) {
        this.userId = userId;
        this.googleId = googleId;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.preferredUnit = preferredUnit;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.hasPassword = hasPassword;
    }

    public static UserDto from(User user) {
        if (user == null) return null;
        return new UserDto(user.getUserId(), user.getGoogleId(), user.getEmail(),
                user.getDisplayName(), user.getAvatarUrl(), user.getRole(),
                user.getPreferredUnit(), user.getCreatedAt(), user.getLastLoginAt(),
                user.getPasswordHash() != null && !user.getPasswordHash().isEmpty());
    }

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
    public boolean isHasPassword() { return hasPassword; }
    public void setHasPassword(boolean hasPassword) { this.hasPassword = hasPassword; }
}

