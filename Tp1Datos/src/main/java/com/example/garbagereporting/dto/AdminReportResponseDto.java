package com.example.garbagereporting.dto;

import com.example.garbagereporting.model.AdminReportStatus;
import com.example.garbagereporting.model.ReportStatus;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminReportResponseDto {
    String id;
    String requestId;
    String userId;
    String userEmail;
    String user;
    String imageUrl;
    LocationResponseDto location;
    Instant createdAt;
    ReportStatus status;
    AdminReportStatus adminStatus;
    Instant adminStatusUpdatedAt;
    String classificationResult;
    boolean isTrash;
}
