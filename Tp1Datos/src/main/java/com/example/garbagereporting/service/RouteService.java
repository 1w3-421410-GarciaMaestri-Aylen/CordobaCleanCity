package com.example.garbagereporting.service;

import com.example.garbagereporting.dto.OptimalRouteResponseDto;
import com.example.garbagereporting.dto.RouteScope;

public interface RouteService {

    OptimalRouteResponseDto getOptimalRoute(RouteScope scope, Double startLat, Double startLng);

    void evictOptimalRoutesCache();
}
