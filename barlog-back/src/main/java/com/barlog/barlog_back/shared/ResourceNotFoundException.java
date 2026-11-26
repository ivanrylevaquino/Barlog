package com.barlog.barlog_back.shared;

/** Thrown when a resource does not exist or is not owned by the caller (maps to 404, so ownership is not leaked). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
