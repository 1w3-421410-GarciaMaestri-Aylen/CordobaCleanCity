package com.example.garbagereporting.controller;

import com.example.garbagereporting.dto.OptimalRouteResponseDto;
import com.example.garbagereporting.dto.RouteScope;
import com.example.garbagereporting.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    @GetMapping("/optimal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OptimalRouteResponseDto> getOptimalRoute(
            @RequestParam(defaultValue = "today") String scope,
            @RequestParam Double startLat,
            @RequestParam Double startLng
    ) {
        RouteScope routeScope = RouteScope.from(scope);
        return ResponseEntity.ok(routeService.getOptimalRoute(routeScope, startLat, startLng));
    }
}
