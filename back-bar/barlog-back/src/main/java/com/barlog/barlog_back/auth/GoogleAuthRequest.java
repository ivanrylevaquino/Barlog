package com.barlog.barlog_back.auth;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /api/auth/google. */
public class GoogleAuthRequest {

    @NotBlank(message = "Google ID token must not be blank")
    private String idToken;

    public GoogleAuthRequest() {}
    public GoogleAuthRequest(String idToken) { this.idToken = idToken; }

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
}
