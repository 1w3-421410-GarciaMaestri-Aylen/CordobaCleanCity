import { useEffect, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import type { ReportSource, RouteScope, UploadReportPayload } from "../types/api";

interface UploadPanelProps {
  currentUserLabel: string;
  loadingUpload: boolean;
  loadingReports: boolean;
  loadingRoute: boolean;
  reportSource: ReportSource;
  onReportSourceChange: (value: ReportSource) => void;
  onSubmit: (payload: UploadReportPayload) => Promise<void>;
  onRefreshReports: (source: ReportSource) => Promise<void>;
  onGenerateRoute: (scope: RouteScope) => Promise<void>;
  onOpenMap: () => void;
  onGenerateQr: () => void;
  reportCount: number;
  routePoints: number;
  totalDistance: number;
}

const DEFAULT_LAT = "-34.6037";
const DEFAULT_LNG = "-58.3816";

export function UploadPanel({
  currentUserLabel,
  loadingUpload,
  loadingReports,
  loadingRoute,
  reportSource,
  onReportSourceChange,
  onSubmit,
  onRefreshReports,
  onGenerateRoute,
  onOpenMap,
  onGenerateQr,
  reportCount,
  routePoints,
  totalDistance
}: UploadPanelProps): JSX.Element {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState("");
  const [lat, setLat] = useState(DEFAULT_LAT);
  const [lng, setLng] = useState(DEFAULT_LNG);
  const [scope, setScope] = useState<RouteScope>("today");

  useEffect(() => {
    if (!selectedFile) {
      setPreviewUrl("");
      return;
    }

    const nextPreviewUrl = URL.createObjectURL(selectedFile);
    setPreviewUrl(nextPreviewUrl);

    return () => {
      URL.revokeObjectURL(nextPreviewUrl);
    };
  }, [selectedFile]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (!selectedFile) {
      return;
    }

    await onSubmit({
      image: selectedFile,
      user: currentUserLabel,
      lat: Number(lat),
      lng: Number(lng)
    });
  };

  const onFileChange = (event: ChangeEvent<HTMLInputElement>): void => {
    const nextFile = event.target.files?.[0] ?? null;
    setSelectedFile(nextFile);
  };

  return (
    <section className="card upload-card">
      <div className="stats-grid">
        <article className="stat-item">
          <span className="stat-label">Reportes</span>
          <strong>{reportCount}</strong>
        </article>
        <article className="stat-item">
          <span className="stat-label">Puntos de ruta</span>
          <strong>{routePoints}</strong>
        </article>
        <article className="stat-item">
          <span className="stat-label">Distancia</span>
          <strong>{totalDistance.toFixed(2)} km</strong>
        </article>
      </div>

      <form className="upload-form" onSubmit={handleSubmit}>
        <div className="field-label">
          <span>Foto del reporte</span>
          <label className="file-trigger" htmlFor="report-image-input">
            <input id="report-image-input" type="file" accept="image/*" onChange={onFileChange} required />
            <span>{selectedFile ? "Cambiar imagen" : "Seleccionar imagen"}</span>
          </label>
        </div>

        <div className="preview-box">
          {previewUrl ? <img src={previewUrl} alt="Preview de imagen" /> : <span>Preview de imagen</span>}
        </div>

        <p className="current-user">Usuario autenticado: {currentUserLabel}</p>

        <div className="coordinate-grid">
          <label className="field-label">
            Latitud
            <input
              type="number"
              value={lat}
              onChange={(event) => setLat(event.target.value)}
              step="0.000001"
              required
            />
          </label>
          <label className="field-label">
            Longitud
            <input
              type="number"
              value={lng}
              onChange={(event) => setLng(event.target.value)}
              step="0.000001"
              required
            />
          </label>
        </div>

        <button type="submit" className="btn btn-primary" disabled={loadingUpload || !selectedFile}>
          {loadingUpload ? "Subiendo..." : "Enviar reporte"}
        </button>

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
              className={`segment ${scope === "today" ? "is-active" : ""}`}
              onClick={() => setScope("today")}
            >
              Hoy
            </button>
            <button
              type="button"
              className={`segment ${scope === "active" ? "is-active" : ""}`}
              onClick={() => setScope("active")}
            >
              Activos
            </button>
          </div>
        </div>

        <div className="actions-grid">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => void onRefreshReports(reportSource)}
            disabled={loadingReports}
          >
            {loadingReports ? "Actualizando..." : "Actualizar reportes"}
          </button>

          <button type="button" className="btn btn-accent" onClick={() => void onGenerateRoute(scope)} disabled={loadingRoute}>
            {loadingRoute ? "Generando..." : "Generar ruta"}
          </button>

          <button type="button" className="btn btn-ghost" onClick={onOpenMap}>
            Ver mapa
          </button>

          <button type="button" className="btn btn-outline" onClick={onGenerateQr}>
            Generar QR
          </button>
        </div>
      </form>
    </section>
  );
}
