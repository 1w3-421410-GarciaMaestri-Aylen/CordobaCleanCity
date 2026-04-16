package com.example.garbagereporting.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RouteOrderedPointDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    Integer sequence;
    String reportId;
    String user;
    RouteCoordinateDto coordinate;
    Instant createdAt;
}
