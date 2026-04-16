package com.example.garbagereporting.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptimalRouteResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    List<RouteOrderedPointDto> orderedPoints;
    RouteCoordinateDto startCoordinate;
    List<RouteCoordinateDto> coordinates;
    List<RouteCoordinateDto> geometry;
    Double totalDistanceKm;
    RouteMetadataDto metadata;
    Instant generatedAt;
}
