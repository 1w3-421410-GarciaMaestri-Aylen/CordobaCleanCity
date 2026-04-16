package com.example.garbagereporting.dto;

import com.example.garbagereporting.exception.BusinessValidationException;
import java.util.Locale;

public enum RouteScope {
    TODAY,
    ACTIVE;

    public static RouteScope from(String raw) {
        if (raw == null || raw.isBlank()) {
            return TODAY;
        }
        try {
            return RouteScope.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException("Invalid route scope. Use 'today' or 'active'");
        }
    }
}
