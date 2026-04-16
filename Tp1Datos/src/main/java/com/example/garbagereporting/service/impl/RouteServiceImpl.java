package com.example.garbagereporting.service.impl;

import com.example.garbagereporting.cache.CacheNames;
import com.example.garbagereporting.exception.BusinessValidationException;
import com.example.garbagereporting.dto.OptimalRouteResponseDto;
import com.example.garbagereporting.dto.RouteCoordinateDto;
import com.example.garbagereporting.dto.RouteMetadataDto;
import com.example.garbagereporting.dto.RouteOrderedPointDto;
import com.example.garbagereporting.dto.RouteScope;
import com.example.garbagereporting.model.AdminReportStatus;
import com.example.garbagereporting.model.Location;
import com.example.garbagereporting.model.Report;
import com.example.garbagereporting.model.ReportStatus;
import com.example.garbagereporting.repository.ReportRepository;
import com.example.garbagereporting.service.RoadRoutingService;
import com.example.garbagereporting.service.RouteService;
import com.example.garbagereporting.utils.GeoUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private static final List<ReportStatus> ACTIVE_STATUSES = List.of(
            ReportStatus.PROCESSED_VALID,
            ReportStatus.CONFIRMED,
            ReportStatus.PROCESSED
    );
    private static final String ALGORITHM = "nearest-neighbor-haversine";
    private static final String ROUTE_CACHE_VERSION = "v3";

    private final ReportRepository reportRepository;
    private final RoadRoutingService roadRoutingService;

    @Override
    @Cacheable(
            cacheNames = CacheNames.ROUTE_OPTIMIZATION,
            key = "T(String).format('%s:%s:%.6f:%.6f', '" + ROUTE_CACHE_VERSION + "', #scope.name(), #startLat, #startLng)",
            unless = "#result.metadata.routingStatus == 'failed'"
    )
    public OptimalRouteResponseDto getOptimalRoute(RouteScope scope, Double startLat, Double startLng) {
        RouteCoordinateDto startCoordinate = validateAndBuildStartCoordinate(startLat, startLng);
        List<Report> sourceReports = fetchPendingReports(scope).stream()
                .filter(this::hasValidLocation)
                .toList();
        log.info(
                "Route request scope={} startLat={} startLng={} validPendingReports={}",
                scope.name().toLowerCase(Locale.ROOT),
                startLat,
                startLng,
                sourceReports.size()
        );

        if (sourceReports.isEmpty()) {
            return OptimalRouteResponseDto.builder()
                    .orderedPoints(List.of())
                    .startCoordinate(startCoordinate)
                    .coordinates(List.of())
                    .geometry(List.of())
                    .totalDistanceKm(0.0)
                    .metadata(buildMetadata(scope, 0, "none", "empty", "No hay reportes pendientes para recolectar."))
                    .generatedAt(Instant.now())
                    .build();
        }

        List<Report> orderedReports = buildNearestNeighborPath(sourceReports, startCoordinate);
        List<RouteOrderedPointDto> orderedPoints = toOrderedPoints(orderedReports);
        List<RouteCoordinateDto> reportCoordinates = orderedPoints.stream()
                .map(RouteOrderedPointDto::getCoordinate)
                .toList();
        List<RouteCoordinateDto> routingCoordinates = new ArrayList<>(reportCoordinates.size() + 1);
        routingCoordinates.add(startCoordinate);
        routingCoordinates.addAll(reportCoordinates);
        double fallbackDistanceKm = calculateTotalDistance(routingCoordinates);

        log.info(
                "Route optimization scope={} waypointCount={} start={} waypoints={}",
                scope.name().toLowerCase(Locale.ROOT),
                reportCoordinates.size(),
                startCoordinate,
                reportCoordinates
        );

        RoadRoutingService.RoadRoutingResult routingResult = roadRoutingService.buildRoute(routingCoordinates);
        List<RouteCoordinateDto> geometry = routingResult.geometry() == null ? List.of() : routingResult.geometry();
        double totalDistanceKm = "success".equals(routingResult.status())
                ? routingResult.totalDistanceKm()
                : fallbackDistanceKm;

        log.info(
                "Route routing scope={} status={} provider={} geometryPoints={} firstGeometryPoint={} lastGeometryPoint={} totalDistanceKm={}",
                scope.name().toLowerCase(Locale.ROOT),
                routingResult.status(),
                routingResult.provider(),
                geometry.size(),
                geometry.isEmpty() ? null : geometry.getFirst(),
                geometry.isEmpty() ? null : geometry.getLast(),
                totalDistanceKm
        );

        return OptimalRouteResponseDto.builder()
                .orderedPoints(orderedPoints)
                .startCoordinate(startCoordinate)
                .coordinates(routingCoordinates)
                .geometry(geometry)
                .totalDistanceKm(totalDistanceKm)
                .metadata(buildMetadata(
                        scope,
                        sourceReports.size(),
                        routingResult.provider(),
                        routingResult.status(),
                        routingResult.message()
                ))
                .generatedAt(Instant.now())
                .build();
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.ROUTE_OPTIMIZATION, allEntries = true)
    public void evictOptimalRoutesCache() {
    }

    private List<Report> fetchPendingReports(RouteScope scope) {
        List<Report> reports;
        if (scope == RouteScope.ACTIVE) {
            reports = reportRepository.findByStatusInOrderByCreatedAtDesc(ACTIVE_STATUSES);
        } else {
            ZoneId zoneId = ZoneId.systemDefault();
            LocalDate today = LocalDate.now(zoneId);
            Instant from = today.atStartOfDay(zoneId).toInstant();
            Instant to = today.plusDays(1).atStartOfDay(zoneId).toInstant();

            reports = reportRepository.findByCreatedAtRangeAndStatusInOrderByCreatedAtDesc(
                    from,
                    to,
                    ACTIVE_STATUSES
            );
        }

        return reports.stream()
                .filter(report -> report.getAdminStatus() == AdminReportStatus.PENDING)
                .toList();
    }

    private List<Report> buildNearestNeighborPath(List<Report> reports, RouteCoordinateDto startCoordinate) {
        List<Report> unvisited = new ArrayList<>(reports);
        unvisited.sort(Comparator.comparing(Report::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Report> ordered = new ArrayList<>();
        RouteCoordinateDto currentCoordinate = startCoordinate;

        while (!unvisited.isEmpty()) {
            RouteCoordinateDto currentRef = currentCoordinate;
            Report nearest = unvisited.stream()
                    .min(Comparator.comparingDouble(candidate -> distance(currentRef, candidate)))
                    .orElseThrow();
            ordered.add(nearest);
            unvisited.remove(nearest);
            currentCoordinate = toCoordinate(nearest.getLocation());
        }

        return ordered;
    }

    private List<RouteOrderedPointDto> toOrderedPoints(List<Report> orderedReports) {
        List<RouteOrderedPointDto> points = new ArrayList<>(orderedReports.size());
        int sequence = 1;
        for (Report report : orderedReports) {
            points.add(RouteOrderedPointDto.builder()
                    .sequence(sequence++)
                    .reportId(report.getId())
                    .user(resolveUserLabel(report))
                    .coordinate(toCoordinate(report.getLocation()))
                    .createdAt(report.getCreatedAt())
                    .build());
        }
        return points;
    }

    private double distance(RouteCoordinateDto from, Report to) {
        RouteCoordinateDto toCoordinate = toCoordinate(to.getLocation());
        return GeoUtils.haversineKm(
                from.getLat(),
                from.getLng(),
                toCoordinate.getLat(),
                toCoordinate.getLng()
        );
    }

    private RouteCoordinateDto toCoordinate(Location location) {
        if (location == null) {
            return RouteCoordinateDto.builder().lat(0.0).lng(0.0).build();
        }
        return RouteCoordinateDto.builder()
                .lat(location.getLat())
                .lng(location.getLng())
                .build();
    }

    private double calculateTotalDistance(List<RouteCoordinateDto> orderedCoordinates) {
        if (orderedCoordinates.size() <= 1) {
            return 0.0;
        }

        double total = 0.0;
        for (int i = 1; i < orderedCoordinates.size(); i++) {
            RouteCoordinateDto from = orderedCoordinates.get(i - 1);
            RouteCoordinateDto to = orderedCoordinates.get(i);
            total += GeoUtils.haversineKm(from.getLat(), from.getLng(), to.getLat(), to.getLng());
        }
        return total;
    }

    private boolean hasValidLocation(Report report) {
        if (report == null || report.getLocation() == null) {
            return false;
        }

        return GeoUtils.isValidCoordinate(report.getLocation().getLat(), report.getLocation().getLng());
    }

    private RouteMetadataDto buildMetadata(
            RouteScope scope,
            int sourceReportCount,
            String routingProvider,
            String routingStatus,
            String routingMessage
    ) {
        return RouteMetadataDto.builder()
                .algorithm(ALGORITHM)
                .scope(scope.name().toLowerCase(Locale.ROOT))
                .sourceReportCount(sourceReportCount)
                .routingProvider(routingProvider)
                .routingStatus(routingStatus)
                .routingMessage(routingMessage)
                .build();
    }

    private String resolveUserLabel(Report report) {
        if (report.getUser() != null && !report.getUser().isBlank()) {
            return report.getUser();
        }
        if (report.getUserEmail() != null && !report.getUserEmail().isBlank()) {
            return report.getUserEmail();
        }
        return "usuario";
    }

    private RouteCoordinateDto validateAndBuildStartCoordinate(Double startLat, Double startLng) {
        if (!GeoUtils.isValidCoordinate(startLat, startLng)) {
            throw new BusinessValidationException("La ubicacion inicial del recolector es invalida.");
        }
        if (!GeoUtils.isInsideCordobaCapital(startLat, startLng)) {
            throw new BusinessValidationException("La ubicacion inicial debe estar dentro de Cordoba Capital.");
        }

        return RouteCoordinateDto.builder()
                .lat(startLat)
                .lng(startLng)
                .build();
    }
}
