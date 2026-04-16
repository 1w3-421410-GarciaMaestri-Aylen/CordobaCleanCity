package com.example.garbagereporting.security;

import java.time.Instant;

public record JwtTokenResult(String tokenValue, Instant expiresAt) {
}
