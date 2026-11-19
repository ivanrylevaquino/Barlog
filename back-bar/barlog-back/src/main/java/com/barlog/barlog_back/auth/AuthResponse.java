package com.barlog.barlog_back.auth;

/** Response body for POST /api/auth/google. */
public class AuthResponse {
    private String token;
    private UserDto user;

    public AuthResponse() {}
    public AuthResponse(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
}

