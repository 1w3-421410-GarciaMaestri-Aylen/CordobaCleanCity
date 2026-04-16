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
@Document(collection = "garbage_reports")
@CompoundIndex(name = "status_created_idx", def = "{'status': 1, 'createdAt': -1}")
public class GarbageReport {

    @Id
    private String id;

    private String imageUrl;

    private double latitude;

    private double longitude;

    private GarbageCategory predictedCategory;

    private Double confidence;

    private ReportStatus status;

    private Instant createdAt;

    private Instant updatedAt;
}
