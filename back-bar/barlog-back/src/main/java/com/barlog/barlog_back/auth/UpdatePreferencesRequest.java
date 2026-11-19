package com.barlog.barlog_back.auth;

import jakarta.validation.constraints.NotNull;

/** Request body for PATCH /api/auth/me/preferences. */
public class UpdatePreferencesRequest {

    @NotNull(message = "preferredUnit is required (KG or LB)")
    private PreferredUnit preferredUnit;

    public UpdatePreferencesRequest() {}
    public UpdatePreferencesRequest(PreferredUnit preferredUnit) { this.preferredUnit = preferredUnit; }

    public PreferredUnit getPreferredUnit() { return preferredUnit; }
    public void setPreferredUnit(PreferredUnit preferredUnit) { this.preferredUnit = preferredUnit; }
}
