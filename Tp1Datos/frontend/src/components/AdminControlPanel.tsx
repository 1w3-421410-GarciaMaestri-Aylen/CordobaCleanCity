import type { ReportSource, RouteScope } from "../types/api";

interface AdminControlPanelProps {
  open: boolean;
  reportSource: ReportSource;
  routeScope: RouteScope;
  reportCount: number;
  routePoints: number;
  totalDistanceKm: number;
  loadingReports: boolean;
  loadingRoute: boolean;
  onClose: () => void;
  onReportSourceChange: (value: ReportSource) => void;
  onRouteScopeChange: (value: RouteScope) => void;
  onRefreshReports: () => Promise<void>;
  onGenerateRoute: () => Promise<void>;
}

export function AdminControlPanel({
  open,
  reportSource,
  routeScope,
  reportCount,
  routePoints,
  totalDistanceKm,
  loadingReports,
  loadingRoute,
  onClose,
  onReportSourceChange,
  onRouteScopeChange,
  onRefreshReports,
  onGenerateRoute
}: AdminControlPanelProps): JSX.Element | null {
  if (!open) {
    return null;
  }

  return (
    <div className="admin-panel-backdrop" role="presentation" onClick={onClose}>
      <aside
        className="admin-panel"
        role="dialog"
        aria-modal="true"
        aria-label="Panel administrativo"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="admin-panel-header">
          <div>
            <h2>Panel admin</h2>
            <p>Controles avanzados del sistema</p>
          </div>
          <button type="button" className="btn btn-outline compact-btn" onClick={onClose}>
            Cerrar
          </button>
        </header>

        <div className="admin-metrics-grid">
          <article className="admin-metric-item">
            <span>Reportes</span>
            <strong>{reportCount}</strong>
          </article>
          <article className="admin-metric-item">
            <span>Puntos de ruta</span>
            <strong>{routePoints}</strong>
          </article>
          <article className="admin-metric-item">
            <span>Distancia</span>
            <strong>{totalDistanceKm.toFixed(2)} km</strong>
          </article>
        </div>

        <div className="inline-group">
          <span className="group-label">Fuente de reportes</span>
          <div className="segmented-control">
            <button
              type="button"
              className={`segment ${reportSource === "today" ? "is-active" : ""}`}
              onClick={() => onReportSourceChange("today")}
            >
              Solo hoy
            </button>
            <button
              type="button"
              className={`segment ${reportSource === "all" ? "is-active" : ""}`}
              onClick={() => onReportSourceChange("all")}
            >
              Todos
            </button>
          </div>
        </div>

        <div className="inline-group">
          <span className="group-label">Alcance de ruta</span>
          <div className="segmented-control">
            <button
              type="button"
              className={`segment ${routeScope === "today" ? "is-active" : ""}`}
              onClick={() => onRouteScopeChange("today")}
            >
              Hoy
            </button>
            <button
              type="button"
              className={`segment ${routeScope === "active" ? "is-active" : ""}`}
              onClick={() => onRouteScopeChange("active")}
            >
              Activos
            </button>
          </div>
        </div>

        <div className="admin-actions-grid">
          <button
            type="button"
            className="btn btn-secondary"
            disabled={loadingReports}
            onClick={() => void onRefreshReports()}
          >
            {loadingReports ? "Actualizando..." : "Actualizar reportes"}
          </button>

          <button
            type="button"
            className="btn btn-accent"
            disabled={loadingRoute}
            onClick={() => void onGenerateRoute()}
          >
            {loadingRoute ? "Generando..." : "Generar ruta"}
          </button>
        </div>
      </aside>
    </div>
  );
}
