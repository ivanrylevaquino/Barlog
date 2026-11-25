package com.barlog.barlog_back.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PasswordResetRequest {
    @Email
    @NotBlank
    private String email;

    public PasswordResetRequest() {}
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
