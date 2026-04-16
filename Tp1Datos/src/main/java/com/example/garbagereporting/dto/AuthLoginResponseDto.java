package com.example.garbagereporting.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthLoginResponseDto {
    String tokenType;
    String accessToken;
    Instant expiresAt;
    AuthUserResponseDto user;
}
