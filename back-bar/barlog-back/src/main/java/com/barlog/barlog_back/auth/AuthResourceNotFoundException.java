package com.barlog.barlog_back.auth;

/** Thrown when a requested user record cannot be found. */
public class AuthResourceNotFoundException extends RuntimeException {
    public AuthResourceNotFoundException(String message) {
        super(message);
    }
}
