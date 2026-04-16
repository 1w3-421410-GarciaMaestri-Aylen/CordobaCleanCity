package com.example.garbagereporting.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmailVerificationResponseDto {
    boolean verified;
    String message;
}
