import { useEffect } from "react";
import L from "leaflet";
import { MapContainer, Marker, Polyline, Popup, TileLayer, useMap } from "react-leaflet";
import type { LatLngExpression } from "leaflet";
import { createReportMarkerIcon, createRouteStartIcon, resolveImageUrl } from "./mapMarkerIcons";
import { CORDOBA_DEFAULT_ZOOM, CORDOBA_MAP_BOUNDS, CORDOBA_MAP_CENTER, CORDOBA_MAX_ZOOM, CORDOBA_MIN_ZOOM } from "../constants/mapConfig";
import type { OptimalRouteResponse, ReportDto, RouteCoordinate } from "../types/api";
import { buildReportImageFallbackDataUri } from "../utils/reportImages";

const DEFAULT_CENTER: LatLngExpression = CORDOBA_MAP_CENTER;

L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png"
});

interface ReportMapProps {
  reports: ReportDto[];
  route: OptimalRouteResponse | null;
  className?: string;
}

interface FitBoundsProps {
  reports: ReportDto[];
  route: OptimalRouteResponse | null;
}

function isValidCoordinate(lat: number, lng: number): boolean {
  return Number.isFinite(lat) && Number.isFinite(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
}

type RenderableRouteDecision = {
  source: "geometry" | "fallback_coordinates" | "no_route";
  points: [number, number][];
};

function resolveRenderableRoute(route: OptimalRouteResponse | null): RenderableRouteDecision {
  const geometryPoints = (route?.geometry ?? [])
    .filter((coordinate) => isValidCoordinate(coordinate.lat, coordinate.lng))
    .map((coordinate) => [coordinate.lat, coordinate.lng] as [number, number]);
  const fallbackPoints = (route?.coordinates ?? [])
    .filter((coordinate) => isValidCoordinate(coordinate.lat, coordinate.lng))
    .map((coordinate) => [coordinate.lat, coordinate.lng] as [number, number]);
  const routingStatus = route?.metadata?.routingStatus ?? null;

  if (!route) {
    return { source: "no_route", points: [] };
  }

  if (routingStatus === "success") {
    if (geometryPoints.length > 1) {
      return { source: "geometry", points: geometryPoints };
    }

    console.error("[ReportMap] inconsistent route payload: routingStatus=success but geometry is empty or invalid", {
      routingStatus,
      geometryLength: route.geometry?.length ?? 0,
      coordinatesLength: route.coordinates?.length ?? 0,
      firstGeometryPoint: route.geometry?.[0] ?? null,
      lastGeometryPoint: route.geometry?.[route.geometry.length - 1] ?? null
    });
    return { source: "no_route", points: [] };
  }

  if (routingStatus === "failed") {
    return { source: "fallback_coordinates", points: fallbackPoints };
  }

  if (routingStatus === "single_point" || routingStatus === "empty") {
    return { source: "no_route", points: [] };
  }

  console.error("[ReportMap] unknown routing status, skipping polyline render", {
    routingStatus,
    geometryLength: route.geometry?.length ?? 0,
    coordinatesLength: route.coordinates?.length ?? 0
  });
  return { source: "no_route", points: [] };
}

function FitBounds({ reports, route }: FitBoundsProps): null {
  const map = useMap();

  useEffect(() => {
    const startCoordinate = route?.startCoordinate;
    const startPoint = startCoordinate && isValidCoordinate(startCoordinate.lat, startCoordinate.lng)
      ? ([startCoordinate.lat, startCoordinate.lng] as [number, number])
      : null;
    const routeDecision = resolveRenderableRoute(route);
    const routePoints = routeDecision.points;
    const reportPoints = reports
      .filter((report) => isValidCoordinate(report.location.lat, report.location.lng))
      .map((report) => [report.location.lat, report.location.lng] as [number, number]);
    const points = routePoints.length ? [...routePoints, ...reportPoints] : reportPoints;
    if (startPoint) {
      points.push(startPoint);
    }

    if (!points.length) {
      map.setView(DEFAULT_CENTER, CORDOBA_DEFAULT_ZOOM);
      return;
    }

    if (points.length === 1) {
      map.setView(points[0], 16);
      return;
    }

    const bounds = L.latLngBounds(points);
    map.fitBounds(bounds, { padding: [40, 40] });
  }, [map, reports, route]);

  return null;
}

export function ReportMap({ reports, route, className }: ReportMapProps): JSX.Element {
  const routeDecision = resolveRenderableRoute(route);
  const renderableRoute = routeDecision.points;
  const routePath: LatLngExpression[] = renderableRoute.map((coordinate) => [coordinate[0], coordinate[1]]);
  const markerReports = reports.filter((report) => isValidCoordinate(report.location.lat, report.location.lng));
  const imageFallbackSrc = buildReportImageFallbackDataUri();
  const routeStartPoint: RouteCoordinate | null =
    route?.startCoordinate && isValidCoordinate(route.startCoordinate.lat, route.startCoordinate.lng)
      ? route.startCoordinate
      : null;

  useEffect(() => {
    console.debug("[ReportMap] route render source", routeDecision.source);
    console.debug("[ReportMap] route input", {
      reportCount: reports.length,
      routeStatus: route?.metadata?.routingStatus ?? null,
      routeProvider: route?.metadata?.routingProvider ?? null,
      geometryLength: route?.geometry?.length ?? 0,
      coordinatesLength: route?.coordinates?.length ?? 0,
      firstGeometryPoint: route?.geometry?.[0] ?? null,
      lastGeometryPoint: route?.geometry?.[route.geometry.length - 1] ?? null,
      firstCoordinatePoint: route?.coordinates?.[0] ?? null,
      lastCoordinatePoint: route?.coordinates?.[route.coordinates.length - 1] ?? null,
      polylineSource: routeDecision.source,
      polylinePoints: routePath.length,
      renderableRouteLength: renderableRoute.length,
      firstRenderablePoint: renderableRoute[0] ?? null,
      lastRenderablePoint: renderableRoute[renderableRoute.length - 1] ?? null
    });
  }, [renderableRoute, reports.length, route, routeDecision.source]);

  return (
    <MapContainer
      center={DEFAULT_CENTER}
      zoom={CORDOBA_DEFAULT_ZOOM}
      minZoom={CORDOBA_MIN_ZOOM}
      maxZoom={CORDOBA_MAX_ZOOM}
      maxBounds={CORDOBA_MAP_BOUNDS}
      maxBoundsViscosity={1}
      scrollWheelZoom
      className={className ?? "report-map"}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      <FitBounds reports={reports} route={route} />

      {markerReports.map((report) => (
        <Marker
          key={report.id}
          position={[report.location.lat, report.location.lng]}
          icon={createReportMarkerIcon(report)}
          eventHandlers={{
            add: () => {
              const finalImageSrc = resolveImageUrl(report.imageUrl);
              console.debug("[ReportMap] marker rendered", {
                reportId: report.id,
                imageUrl: report.imageUrl,
                finalImageSrc,
                popupHasImage: Boolean(report.imageUrl)
              });
            }
          }}
        >
          <Popup>
            <div style={{ display: "flex", flexDirection: "column", gap: "8px", minWidth: "180px" }}>
              {report.imageUrl ? (
                <img
                  src={resolveImageUrl(report.imageUrl)}
                  alt={`Reporte ${report.id}`}
                  onError={(event) => {
                    event.currentTarget.onerror = null;
                    event.currentTarget.src = imageFallbackSrc;
                  }}
                  style={{ width: "100%", height: "120px", objectFit: "cover", borderRadius: "8px", display: "block" }}
                />
              ) : null}
              <div>
                <strong>Reporte:</strong> {report.id}
              </div>
              <div>
                <strong>Usuario:</strong> {report.user}
              </div>
              <div>
                <strong>Estado:</strong> {report.status}
              </div>
            </div>
          </Popup>
        </Marker>
      ))}

      {routeStartPoint && (
        <Marker
          position={[routeStartPoint.lat, routeStartPoint.lng]}
          icon={createRouteStartIcon()}
        >
          <Popup>
            <div style={{ display: "flex", flexDirection: "column", gap: "4px", minWidth: "160px" }}>
              <strong>Inicio del recolector</strong>
              <span>
                {routeStartPoint.lat.toFixed(5)}, {routeStartPoint.lng.toFixed(5)}
              </span>
            </div>
          </Popup>
        </Marker>
      )}

      {routePath.length > 1 && <Polyline positions={routePath} pathOptions={{ color: "#1f7f74", weight: 6 }} />}
    </MapContainer>
  );
}
