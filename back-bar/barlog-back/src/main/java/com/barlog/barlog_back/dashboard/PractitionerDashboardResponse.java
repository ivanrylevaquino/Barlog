package com.barlog.barlog_back.dashboard;

import com.barlog.barlog_back.auth.PreferredUnit;
import com.barlog.barlog_back.auth.Role;

import java.time.Instant;
import java.util.UUID;

/** Response body for GET /api/dashboard/practitioner. */
public class PractitionerDashboardResponse {
    private UUID userId;
    private String displayName;
    private String email;
    private String avatarUrl;
    private Role role;
    private PreferredUnit preferredUnit;
    private Instant memberSince;
    private Instant lastLogin;

    public PractitionerDashboardResponse() {}

    public PractitionerDashboardResponse(UUID userId, String displayName, String email, String avatarUrl,
                                          Role role, PreferredUnit preferredUnit,
                                          Instant memberSince, Instant lastLogin) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.preferredUnit = preferredUnit;
        this.memberSince = memberSince;
        this.lastLogin = lastLogin;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public PreferredUnit getPreferredUnit() { return preferredUnit; }
    public void setPreferredUnit(PreferredUnit preferredUnit) { this.preferredUnit = preferredUnit; }
    public Instant getMemberSince() { return memberSince; }
    public void setMemberSince(Instant memberSince) { this.memberSince = memberSince; }
    public Instant getLastLogin() { return lastLogin; }
    public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin; }
}
