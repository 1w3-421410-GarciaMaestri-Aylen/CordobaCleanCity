import { useEffect } from "react";
import { ReportMap } from "./ReportMap";
import type { OptimalRouteResponse, ReportDto, ReportSource } from "../types/api";

interface MapOverlayProps {
  open: boolean;
  reports: ReportDto[];
  route: OptimalRouteResponse | null;
  reportSource: ReportSource;
  onClose: () => void;
}

export function MapOverlay({ open, reports, route, reportSource, onClose }: MapOverlayProps): JSX.Element | null {
  useEffect(() => {
    if (!open) {
      return;
    }

    const previousOverflow = document.body.style.overflow;
    const onEscape = (event: KeyboardEvent): void => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", onEscape);
    };
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  const sourceLabel = reportSource === "today" ? "Solo hoy" : "Todos";
  const routePoints = route?.orderedPoints.length ?? 0;
  const totalDistance = route?.totalDistanceKm ?? 0;

  return (
    <div className="map-overlay-backdrop" role="presentation">
      <section className="map-overlay" role="dialog" aria-modal="true" aria-label="Mapa de reportes">
        <header className="map-overlay-header">
          <div>
            <h2>Mapa de reportes</h2>
            <p>Vista {sourceLabel.toLowerCase()} con ruta optimizada</p>
          </div>
          <button type="button" className="btn btn-outline map-close-btn" onClick={onClose}>
            Cerrar
          </button>
        </header>

        <div className="map-overlay-meta">
          <span>{reports.length} reportes</span>
          <span>{routePoints} puntos en ruta</span>
          <span>{totalDistance.toFixed(2)} km</span>
        </div>

        <div className="map-stage">
          <ReportMap reports={reports} route={route} className="report-map fullscreen-map" />
        </div>
      </section>
    </div>
  );
}
