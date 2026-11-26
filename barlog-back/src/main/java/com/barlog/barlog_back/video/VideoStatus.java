package com.barlog.barlog_back.video;

/** Lifecycle of an uploaded set video (FR-014, FR-015, FR-020). */
public enum VideoStatus {
    UPLOADED,
    PROCESSING,
    COMPLETED,
    FAILED
}
