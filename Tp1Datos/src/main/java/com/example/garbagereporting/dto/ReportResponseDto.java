package com.example.garbagereporting.dto;

import com.example.garbagereporting.model.ReportStatus;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReportResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String id;
    String userId;
    String userEmail;
    String user;
    String imageUrl;
    LocationResponseDto location;
    Instant createdAt;
    ReportStatus status;
    String classificationResult;
    boolean isTrash;
}
