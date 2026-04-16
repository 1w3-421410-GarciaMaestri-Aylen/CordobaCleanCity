package com.example.garbagereporting.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reports")
@CompoundIndex(name = "created_status_idx", def = "{'createdAt': -1, 'status': 1}")
@CompoundIndex(name = "user_created_idx", def = "{'userId': 1, 'createdAt': -1}")
public class Report {

    @Id
    private String id;

    private String requestId;

    private String userId;

    private String userEmail;

    private String user;

    private String imageUrl;

    private Location location;

    private Instant createdAt;

    private ReportStatus status;

    private AdminReportStatus adminStatus;

    private Instant adminStatusUpdatedAt;

    private String classificationResult;

    private boolean isTrash;
}
