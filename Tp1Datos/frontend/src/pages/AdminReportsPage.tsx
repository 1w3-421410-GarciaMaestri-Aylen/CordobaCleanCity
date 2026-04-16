import { useEffect, useMemo, useState } from "react";
import type { ChangeEvent } from "react";
import { useNavigate } from "react-router-dom";
import { AppBrand } from "../components/AppBrand";
import { reverseGeocodeCordoba } from "../services/geocoding";
import { fetchAdminReports, updateAdminReportStatus } from "../services/api";
import { ApiError } from "../services/httpClient";
import type { AdminReportDto, AdminReportStatus, UiMessage } from "../types/api";
import { buildReportImageFallbackDataUri, resolveReportImageUrl } from "../utils/reportImages";

const statusLabel: Record<AdminReportStatus, string> = {
  PENDING: "Pendiente",
  RESOLVED: "Resuelto"
};

type DisplayStatus = "PENDING" | "RESOLVED" | "REJECTED";

const displayStatusLabel: Record<DisplayStatus, string> = {
  PENDING: "Pendiente",
  RESOLVED: "Resuelto",
  REJECTED: "Rechazado"
};

const processingLabel: Record<string, string> = {
  PENDING: "En cola de procesamiento",
  PROCESSED_VALID: "Aprobado por IA",
  PROCESSED_INVALID: "Rechazado por IA",
  CONFIRMED: "Confirmado",
  PROCESSED: "Procesado"
};

function canUpdateAdminStatus(processingStatus: string): boolean {
  return processingStatus === "PROCESSED_VALID" || processingStatus === "CONFIRMED" || processingStatus === "PROCESSED";
}

function getDisplayStatus(report: AdminReportDto): DisplayStatus {
  return report.status === "PROCESSED_INVALID" ? "REJECTED" : report.adminStatus;
}

function formatDate(value: string | null): string {
  if (!value) {
    return "Sin fecha";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "Sin fecha";
  }

  return new Intl.DateTimeFormat("es-AR", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(parsed);
}

function toUiMessage(error: unknown, fallback: string): UiMessage {
  if (error instanceof ApiError || error instanceof Error) {
    return { type: "error", text: error.message };
  }
  return { type: "error", text: fallback };
}

export function AdminReportsPage(): JSX.Element {
  const navigate = useNavigate();
  const [reports, setReports] = useState<AdminReportDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [message, setMessage] = useState<UiMessage | null>(null);
  const [error, setError] = useState("");
  const [updatingId, setUpdatingId] = useState<string | null>(null);
  const [addressBook, setAddressBook] = useState<Record<string, string>>({});
  const imageFallbackSrc = buildReportImageFallbackDataUri();

  const rejectedCount = useMemo(
    () => reports.filter((report) => getDisplayStatus(report) === "REJECTED").length,
    [reports]
  );
  const resolvedCount = useMemo(
    () => reports.filter((report) => getDisplayStatus(report) === "RESOLVED").length,
    [reports]
  );
  const pendingCount = useMemo(
    () => reports.filter((report) => getDisplayStatus(report) === "PENDING").length,
    [reports]
  );

  const loadReports = async (showRefreshFeedback: boolean): Promise<void> => {
    if (showRefreshFeedback) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    try {
      const nextReports = await fetchAdminReports();
      setReports(nextReports);
      setError("");
      if (showRefreshFeedback) {
        setMessage({ type: "success", text: `Se actualizaron ${nextReports.length} reportes.` });
      }
    } catch (err) {
      const nextMessage = toUiMessage(err, "No se pudieron cargar los reportes.");
      setError(nextMessage.text);
      if (showRefreshFeedback) {
        setMessage(nextMessage);
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    void loadReports(false);
  }, []);

  useEffect(() => {
    const missingReports = reports.filter((report) => !addressBook[report.id]);
    if (missingReports.length === 0) {
      return;
    }

    let active = true;

    const resolveAddresses = async (): Promise<void> => {
      for (const report of missingReports) {
        try {
          const resolvedAddress = await reverseGeocodeCordoba(report.location);
          if (!active) {
            return;
          }
          setAddressBook((current) => ({
            ...current,
            [report.id]: resolvedAddress ?? "Direccion no disponible"
          }));
        } catch {
          if (!active) {
            return;
          }
          setAddressBook((current) => ({
            ...current,
            [report.id]: "Direccion no disponible"
          }));
        }
      }
    };

    void resolveAddresses();

    return () => {
      active = false;
    };
  }, [reports, addressBook]);

  const handleStatusChange = async (
    reportId: string,
    event: ChangeEvent<HTMLSelectElement>
  ): Promise<void> => {
    const nextStatus = event.target.value as AdminReportStatus;
    const previousReports = reports;

    setUpdatingId(reportId);
    setReports((current) =>
      current.map((report) => (report.id === reportId ? { ...report, adminStatus: nextStatus } : report))
    );

    try {
      const updatedReport = await updateAdminReportStatus(reportId, nextStatus);
      setReports((current) => current.map((report) => (report.id === reportId ? updatedReport : report)));
      setMessage({
        type: "success",
        text: `Reporte ${updatedReport.requestId || updatedReport.id} marcado como ${statusLabel[nextStatus].toLowerCase()}.`
      });
    } catch (err) {
      setReports(previousReports);
      setMessage(toUiMessage(err, "No se pudo actualizar el estado del reporte."));
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="auth-root admin-reports-root">
      <section className="auth-card admin-card admin-reports-card">
        <header className="workspace-header admin-reports-header">
          <div className="admin-reports-intro">
            <AppBrand variant="admin" />
            <h1>Seguimiento de reportes</h1>
            <p>Panel operativo para auditar altas, validacion IA y estado operativo.</p>
          </div>
          <div className="workspace-actions admin-reports-actions">
            <button
              type="button"
              className="btn btn-secondary compact-btn"
              disabled={refreshing}
              onClick={() => void loadReports(true)}
            >
              {refreshing ? "Actualizando..." : "Actualizar"}
            </button>
            <button type="button" className="btn btn-outline compact-btn" onClick={() => navigate("/admin/users")}>
              Usuarios
            </button>
            <button type="button" className="btn btn-outline compact-btn" onClick={() => navigate("/app")}>
              Cerrar
            </button>
          </div>
        </header>

        <div className="admin-reports-metrics">
          <article className="admin-report-metric">
            <span>Total</span>
            <strong>{reports.length}</strong>
          </article>
          <article className="admin-report-metric">
            <span>Pendientes</span>
            <strong>{pendingCount}</strong>
          </article>
          <article className="admin-report-metric">
            <span>Resueltos</span>
            <strong>{resolvedCount}</strong>
          </article>
          <article className="admin-report-metric admin-report-metric--rejected">
            <span>Rechazados</span>
            <strong>{rejectedCount}</strong>
          </article>
        </div>

        {message && (
          <div className={`status-banner status-${message.type}`}>
            <strong>{message.type === "success" ? "Exito:" : "Info:"}</strong>
            {message.text}
          </div>
        )}

        {loading && <div className="status-banner status-info"><strong>Info:</strong> Cargando reportes...</div>}

        {!loading && error && (
          <div className="admin-reports-state">
            <div className="status-banner status-error">
              <strong>Error:</strong>
              {error}
            </div>
          </div>
        )}

        {!loading && !error && reports.length === 0 && (
          <div className="admin-reports-state admin-reports-empty">
            <h2>No hay reportes</h2>
            <p>Los nuevos reportes van a aparecer aca aunque sigan pendientes o sean rechazados por la IA.</p>
          </div>
        )}

        {!loading && !error && reports.length > 0 && (
          <div className="admin-reports-grid">
            {reports.map((report) => {
              const isUpdating = updatingId === report.id;
              const address = addressBook[report.id] ?? "Resolviendo direccion...";
              const canOperate = canUpdateAdminStatus(report.status);
              const displayStatus = getDisplayStatus(report);
              const imageSrc = resolveReportImageUrl(report.imageUrl) || imageFallbackSrc;

              return (
                <article key={report.id} className="admin-report-card">
                  <div className="admin-report-media">
                    <img
                      src={imageSrc}
                      alt={`Reporte ${report.requestId || report.id}`}
                      onError={(event) => {
                        event.currentTarget.onerror = null;
                        event.currentTarget.src = imageFallbackSrc;
                      }}
                    />
                  </div>

                  <div className="admin-report-content">
                    <div className="admin-report-topline">
                      <div>
                        <p className="admin-report-eyebrow">Reporte {report.requestId || report.id}</p>
                        <h2>{address}</h2>
                      </div>
                      <span className={`admin-status-badge admin-status-${displayStatus.toLowerCase()}`}>
                        {displayStatusLabel[displayStatus]}
                      </span>
                    </div>

                    <div className="admin-report-meta">
                      <span>{processingLabel[report.status] ?? report.status}</span>
                      <span>{formatDate(report.createdAt)}</span>
                      <span>{report.user}</span>
                    </div>

                    <div className="admin-report-details">
                      <div>
                        <span className="admin-detail-label">Usuario</span>
                        <strong>{report.user}</strong>
                        <small>{report.userEmail}</small>
                      </div>
                      <div>
                        <span className="admin-detail-label">Coordenadas</span>
                        <strong>
                          {report.location.lat.toFixed(5)}, {report.location.lng.toFixed(5)}
                        </strong>
                        <small>ID usuario: {report.userId || "N/D"}</small>
                      </div>
                    </div>

                    <div className="admin-report-actions">
                      {canOperate ? (
                        <label className="field-label admin-status-field">
                          Estado operativo
                          <select
                            value={report.adminStatus}
                            disabled={isUpdating}
                            onChange={(event) => void handleStatusChange(report.id, event)}
                          >
                            <option value="PENDING">Pendiente</option>
                            <option value="RESOLVED">Resuelto</option>
                          </select>
                        </label>
                      ) : (
                        <div className="field-label admin-status-field">
                          Estado operativo
                          <span>
                            {report.status === "PENDING"
                              ? "Esperando clasificacion IA."
                              : "Bloqueado porque la IA lo rechazo."}
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
