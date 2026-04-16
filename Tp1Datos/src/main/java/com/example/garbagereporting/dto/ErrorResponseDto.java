package com.example.garbagereporting.dto;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponseDto {
    Instant timestamp;
    int status;
    String error;
    String message;
    String path;
    Map<String, String> validationErrors;
}
