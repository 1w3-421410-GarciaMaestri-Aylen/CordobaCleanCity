package com.example.garbagereporting.model;

public enum ReportStatus {
    PENDING,
    PROCESSED_VALID,
    PROCESSED_INVALID,
    @Deprecated
    PROCESSED,
    @Deprecated
    REJECTED,
    CONFIRMED
}
