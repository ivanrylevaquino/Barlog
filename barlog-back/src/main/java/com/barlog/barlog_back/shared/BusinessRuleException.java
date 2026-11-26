package com.barlog.barlog_back.shared;

/** Thrown when a request is well-formed but violates a business rule (maps to 409 Conflict). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
