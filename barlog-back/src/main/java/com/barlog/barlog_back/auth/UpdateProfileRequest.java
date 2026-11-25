package com.barlog.barlog_back.auth;

import jakarta.validation.constraints.NotBlank;

public class UpdateProfileRequest {
    private String displayName;
    private String avatarUrl;

    public UpdateProfileRequest() {}
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
