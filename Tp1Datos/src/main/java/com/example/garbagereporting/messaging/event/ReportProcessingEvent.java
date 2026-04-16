package com.example.garbagereporting.messaging.event;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReportProcessingEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String requestId;
    String userId;
    String userEmail;
    String userDisplayName;
    String imageUrl;
    String originalFilename;
    Double lat;
    Double lng;
    Instant submittedAt;
}
