package com.example.garbagereporting.service;

import com.example.garbagereporting.dto.RouteCoordinateDto;
import java.util.List;

public interface RoadRoutingService {

    RoadRoutingResult buildRoute(List<RouteCoordinateDto> orderedCoordinates);

    record RoadRoutingResult(
            List<RouteCoordinateDto> geometry,
            double totalDistanceKm,
            String provider,
            String status,
            String message
    ) {
    }
}
