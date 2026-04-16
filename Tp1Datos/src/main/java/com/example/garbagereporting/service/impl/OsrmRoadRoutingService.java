package com.example.garbagereporting.service.impl;

import com.example.garbagereporting.config.ApplicationProperties;
import com.example.garbagereporting.dto.RouteCoordinateDto;
import com.example.garbagereporting.service.RoadRoutingService;
import com.example.garbagereporting.utils.GeoUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class OsrmRoadRoutingService implements RoadRoutingService {

    private static final String STATUS_EMPTY = "empty";
    private static final String STATUS_SINGLE_POINT = "single_point";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";

    private final ApplicationProperties properties;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public RoadRoutingResult buildRoute(List<RouteCoordinateDto> orderedCoordinates) {
        List<RouteCoordinateDto> validCoordinates = orderedCoordinates == null
                ? List.of()
                : orderedCoordinates.stream()
                .filter(this::isValidCoordinate)
                .toList();

        String provider = properties.getRouting().getProvider().toLowerCase(Locale.ROOT);
        if (validCoordinates.isEmpty()) {
            return new RoadRoutingResult(List.of(), 0.0, provider, STATUS_EMPTY, "No routable points available.");
        }

        if (validCoordinates.size() == 1) {
            return new RoadRoutingResult(validCoordinates, 0.0, provider, STATUS_SINGLE_POINT, "Only one point available.");
        }

        if (!Boolean.TRUE.equals(properties.getRouting().getEnabled())) {
            return new RoadRoutingResult(List.of(), 0.0, provider, STATUS_FAILED, "Routing backend is disabled.");
        }

        if (!"osrm".equalsIgnoreCase(provider)) {
            return new RoadRoutingResult(List.of(), 0.0, provider, STATUS_FAILED, "Unsupported routing provider.");
        }

        try {
            String routeUri = buildRouteUri(validCoordinates);
            log.info("OSRM request provider={} waypointCount={} payload={}", provider, validCoordinates.size(), routeUri);

            byte[] rawResponseBytes = restClient.get()
                    .uri(routeUri)
                    .header(HttpHeaders.ACCEPT_ENCODING, "identity")
                    .retrieve()
                    .body(byte[].class);
            String rawResponse = rawResponseBytes == null ? null : new String(rawResponseBytes, StandardCharsets.UTF_8);

            log.info("OSRM raw response={}", rawResponse);

            OsrmRouteResponse response = rawResponse == null || rawResponse.isBlank()
                    ? null
                    : objectMapper.readValue(rawResponse, OsrmRouteResponse.class);

            if (response != null && response.code() != null && !"Ok".equalsIgnoreCase(response.code())) {
                return new RoadRoutingResult(List.of(), 0.0, provider, STATUS_FAILED, "Routing provider returned non-ok status.");
            }

            if (response == null || response.routes() == null || response.routes().isEmpty()) {
                return new RoadRoutingResult(List.of(), 0.0, provider, STATUS_FAILED, "Routing provider returned no routes.");
            }

            OsrmRouteDto route = response.routes().getFirst();
            if (route.geometry() == null || route.geometry().coordinates() == null || route.geometry().coordinates().isEmpty()) {
                return new RoadRoutingResult(List.of(), 0.0, provider, STATUS_FAILED, "Routing provider returned empty geometry.");
            }

            List<RouteCoordinateDto> geometry = route.geometry().coordinates().stream()
                    .filter(coordinate -> coordinate.size() >= 2)
                    .map(coordinate -> RouteCoordinateDto.builder()
                            .lat(coordinate.get(1))
                            .lng(coordinate.get(0))
                            .build())
                    .filter(this::isValidCoordinate)
                    .toList();

            log.info(
                    "OSRM parsed geometryPoints={} firstGeometryPoint={} lastGeometryPoint={} distanceMeters={}",
                    geometry.size(),
                    geometry.isEmpty() ? null : geometry.getFirst(),
                    geometry.isEmpty() ? null : geometry.getLast(),
                    route.distance()
            );

            if (geometry.size() < 2) {
                return new RoadRoutingResult(List.of(), 0.0, provider, STATUS_FAILED, "Routing provider returned invalid geometry.");
            }

            return new RoadRoutingResult(
                    geometry,
                    route.distance() == null ? 0.0 : route.distance() / 1000.0,
                    provider,
                    STATUS_SUCCESS,
                    null
            );
        } catch (Exception ex) {
            log.warn("OSRM routing request failed: {}", ex.getMessage(), ex);
            return new RoadRoutingResult(List.of(), 0.0, provider, STATUS_FAILED, "Routing provider request failed.");
        }
    }

    private String buildRouteUri(List<RouteCoordinateDto> coordinates) {
        String coordinatePath = coordinates.stream()
                .map(coordinate -> coordinate.getLng() + "," + coordinate.getLat())
                .reduce((left, right) -> left + ";" + right)
                .orElseThrow();

        return UriComponentsBuilder.fromUriString(properties.getRouting().getOsrmBaseUrl())
                .pathSegment("route", "v1", properties.getRouting().getProfile(), coordinatePath)
                .queryParam("overview", "full")
                .queryParam("geometries", "geojson")
                .queryParam("steps", "false")
                .toUriString();
    }

    private boolean isValidCoordinate(RouteCoordinateDto coordinate) {
        return coordinate != null && GeoUtils.isValidCoordinate(coordinate.getLat(), coordinate.getLng());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OsrmRouteResponse(String code, List<OsrmRouteDto> routes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OsrmRouteDto(Double distance, OsrmGeometryDto geometry) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OsrmGeometryDto(List<List<Double>> coordinates, String type) {
    }
}
