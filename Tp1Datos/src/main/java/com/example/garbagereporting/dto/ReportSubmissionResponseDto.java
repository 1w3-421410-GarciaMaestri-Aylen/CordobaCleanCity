package com.example.garbagereporting.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReportSubmissionResponseDto {
    String requestId;
    String status;
    String message;
    String imageUrl;
}
