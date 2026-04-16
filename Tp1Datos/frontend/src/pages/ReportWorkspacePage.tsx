import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { AdminControlPanel } from "../components/AdminControlPanel";
import { AppBrand } from "../components/AppBrand";
import { LocationPickerOverlay } from "../components/LocationPickerOverlay";
import { MapOverlay } from "../components/MapOverlay";
import { StatusBanner } from "../components/StatusBanner";
import { type GeoPoint, isInsideCordobaCapital } from "../constants/location";
import { fetchAllReports, fetchOptimalRoute, fetchTodayReports, submitReport } from "../services/api";
import { reverseGeocodeCordoba, searchCordobaAddressSuggestions, type GeocodingSuggestion } from "../services/geocoding";
import { ApiError } from "../services/httpClient";
import type {
  OptimalRouteResponse,
  ReportDto,
  ReportSource,
  RouteScope,
  UiMessage
} from "../types/api";

const ADDRESS_DEBOUNCE_MS = 400;
const REVERSE_GEOCODE_DEBOUNCE_MS = 450;
const MAX_ADDRESS_SUGGESTIONS = 10;

function toUiMessage(error: unknown, fallback: string): UiMessage {
  if (error instanceof ApiError || error instanceof Error) {
    return { type: "error", text: error.message };
  }
  return { type: "error", text: fallback };
}

function parseCoordinate(value: string): number | null {
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : null;
}

export function ReportWorkspacePage(): JSX.Element {
  const { session, logout } = useAuth();
  const navigate = useNavigate();

  const [selectedImage, setSelectedImage] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState("");
  const previewUrlRef = useRef<string | null>(null);
  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [address, setAddress] = useState("");
  const [addressDirty, setAddressDirty] = useState(false);
  const [addressConfirmed, setAddressConfirmed] = useState(false);
  const [addressSuggestions, setAddressSuggestions] = useState<GeocodingSuggestion[]>([]);
  const [loadingAddressLookup, setLoadingAddressLookup] = useState(false);
  const [addressError, setAddressError] = useState<string | null>(null);
  const [selectedPoint, setSelectedPoint] = useState<GeoPoint | null>(null);
  const [mapOpen, setMapOpen] = useState(false);
  const [loadingSubmit, setLoadingSubmit] = useState(false);
  const [message, setMessage] = useState<UiMessage | null>(null);

  const [adminPanelOpen, setAdminPanelOpen] = useState(false);
  const [adminMapOpen, setAdminMapOpen] = useState(false);
  const [adminRouteStartPickerOpen, setAdminRouteStartPickerOpen] = useState(false);
  const [adminRouteStartPoint, setAdminRouteStartPoint] = useState<GeoPoint | null>(null);
  const [adminReports, setAdminReports] = useState<ReportDto[]>([]);
  const [adminRoute, setAdminRoute] = useState<OptimalRouteResponse | null>(null);
  const [adminReportSource, setAdminReportSource] = useState<ReportSource>("today");
  const [adminRouteScope, setAdminRouteScope] = useState<RouteScope>("today");
  const [loadingAdminReports, setLoadingAdminReports] = useState(false);
  const [loadingAdminRoute, setLoadingAdminRoute] = useState(false);

  const skipCoordinateSyncRef = useRef(false);
  const searchAbortRef = useRef<AbortController | null>(null);
  const reverseAbortRef = useRef<AbortController | null>(null);
  const searchTimerRef = useRef<number | null>(null);
  const reverseTimerRef = useRef<number | null>(null);

  useEffect(() => {
    return () => {
      if (previewUrlRef.current) {
        URL.revokeObjectURL(previewUrlRef.current);
      }
      searchAbortRef.current?.abort();
      reverseAbortRef.current?.abort();
      if (searchTimerRef.current != null) {
        window.clearTimeout(searchTimerRef.current);
      }
      if (reverseTimerRef.current != null) {
        window.clearTimeout(reverseTimerRef.current);
      }
    };
  }, []);

  const scheduleReverseGeocode = (point: GeoPoint): void => {
    reverseAbortRef.current?.abort();
    if (reverseTimerRef.current != null) {
      window.clearTimeout(reverseTimerRef.current);
    }

    reverseTimerRef.current = window.setTimeout(async () => {
      reverseAbortRef.current?.abort();
      const controller = new AbortController();
      reverseAbortRef.current = controller;

      try {
        const reverseAddress = await reverseGeocodeCordoba(point, controller.signal);
        if (reverseAddress) {
          setAddress(reverseAddress);
          setAddressDirty(false);
          setAddressConfirmed(true);
          setAddressSuggestions([]);
          setAddressError(null);
        } else {
          setAddressConfirmed(false);
          setAddressError("No se pudo obtener una direccion valida en Cordoba Capital para ese punto.");
        }
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }
        setAddressError("No se pudo resolver una direccion para esa ubicacion.");
      }
    }, REVERSE_GEOCODE_DEBOUNCE_MS);
  };

  const applyPoint = (point: GeoPoint, nextMessage: string): void => {
    if (!isInsideCordobaCapital(point)) {
      setAddressError("La ubicacion elegida debe estar dentro de Cordoba Capital.");
      return;
    }

    skipCoordinateSyncRef.current = true;
    setLatitude(point.lat.toFixed(6));
    setLongitude(point.lng.toFixed(6));
    setSelectedPoint(point);
    setAddressSuggestions([]);
    setAddressDirty(false);
    setAddressConfirmed(false);
    scheduleReverseGeocode(point);
    setMessage({ type: "info", text: nextMessage });
  };

  useEffect(() => {
    const lat = parseCoordinate(latitude);
    const lng = parseCoordinate(longitude);
    if (lat == null || lng == null) {
      return;
    }

    const point = { lat, lng };
    if (!isInsideCordobaCapital(point)) {
      setAddressError("La ubicacion debe estar dentro de Cordoba Capital.");
      return;
    }

    if (skipCoordinateSyncRef.current) {
      skipCoordinateSyncRef.current = false;
      setAddressError(null);
      return;
    }

    setSelectedPoint((previousPoint) => {
      if (previousPoint && previousPoint.lat === point.lat && previousPoint.lng === point.lng) {
        return previousPoint;
      }
      return point;
    });
    setAddressSuggestions([]);
    setAddressDirty(false);
    setAddressConfirmed(false);
    scheduleReverseGeocode(point);
    setAddressError(null);
  }, [latitude, longitude]);

  useEffect(() => {
    if (!addressDirty) {
      searchAbortRef.current?.abort();
      if (searchTimerRef.current != null) {
        window.clearTimeout(searchTimerRef.current);
      }
      setLoadingAddressLookup(false);
      return;
    }

    const trimmedAddress = address.trim();
    if (trimmedAddress.length < 3) {
      setAddressSuggestions([]);
      setLoadingAddressLookup(false);
      return;
    }

    searchAbortRef.current?.abort();
    if (searchTimerRef.current != null) {
      window.clearTimeout(searchTimerRef.current);
    }

    searchTimerRef.current = window.setTimeout(async () => {
      searchAbortRef.current?.abort();
      const controller = new AbortController();
      searchAbortRef.current = controller;
      setLoadingAddressLookup(true);

      try {
        const suggestions = (await searchCordobaAddressSuggestions(trimmedAddress, controller.signal)).slice(
          0,
          MAX_ADDRESS_SUGGESTIONS
        );
        setAddressSuggestions(suggestions);
        if (suggestions.length === 0) {
          setAddressError("No encontramos esa direccion en Cordoba Capital. Revisa calle y altura.");
        } else {
          setAddressError(null);
        }
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }
        setAddressSuggestions([]);
        setAddressError("No se pudo buscar la direccion. Intenta nuevamente.");
      } finally {
        setLoadingAddressLookup(false);
      }
    }, ADDRESS_DEBOUNCE_MS);
  }, [address, addressDirty]);

  if (!session) {
    return <div className="auth-loading">Cargando sesion...</div>;
  }

  const isAdmin = session.user.role === "ADMIN";

  const fetchReportsBySource = async (source: ReportSource): Promise<ReportDto[]> => {
    return source === "today" ? fetchTodayReports() : fetchAllReports();
  };

  const refreshAdminReports = async (
    showMessage: boolean,
    source: ReportSource = adminReportSource
  ): Promise<void> => {
    setLoadingAdminReports(true);
    try {
      const nextReports = await fetchReportsBySource(source);
      setAdminReports(nextReports);
      if (showMessage) {
        setMessage({ type: "success", text: `Reportes actualizados (${source}): ${nextReports.length}` });
      }
    } catch (error) {
      setMessage(toUiMessage(error, "No se pudieron actualizar reportes."));
    } finally {
      setLoadingAdminReports(false);
    }
  };

  const generateAdminRoute = async (startPoint: GeoPoint): Promise<void> => {
    if (!isInsideCordobaCapital(startPoint)) {
      setMessage({ type: "warning", text: "La ubicacion inicial debe estar dentro de Cordoba Capital." });
      return;
    }

    setLoadingAdminRoute(true);
    try {
      const nextRoute = await fetchOptimalRoute(adminRouteScope, startPoint);
      console.debug("[ReportWorkspacePage] route payload", {
        routingStatus: nextRoute.metadata?.routingStatus ?? null,
        routingProvider: nextRoute.metadata?.routingProvider ?? null,
        geometryLength: nextRoute.geometry?.length ?? 0,
        coordinatesLength: nextRoute.coordinates?.length ?? 0,
        firstGeometryPoint: nextRoute.geometry?.[0] ?? null,
        lastGeometryPoint: nextRoute.geometry?.[nextRoute.geometry.length - 1] ?? null,
        firstCoordinatePoint: nextRoute.coordinates?.[0] ?? null,
        lastCoordinatePoint: nextRoute.coordinates?.[nextRoute.coordinates.length - 1] ?? null
      });
      setAdminRoute(nextRoute);
      setAdminRouteStartPoint(startPoint);
      setAdminPanelOpen(false);
      setAdminMapOpen(true);
      if (nextRoute.orderedPoints.length === 0) {
        setMessage({ type: "warning", text: "No hay reportes pendientes para generar ruta de recoleccion." });
      } else if (nextRoute.metadata.routingStatus === "failed") {
        setMessage({
          type: "warning",
          text: nextRoute.metadata.routingMessage ?? "Se obtuvo el orden de visitas, pero fallo el ruteo por calles."
        });
      } else if (nextRoute.metadata.routingStatus === "single_point") {
        setMessage({ type: "info", text: "Solo hay un punto valido. No se necesita trazar una ruta." });
      } else {
        setMessage({ type: "success", text: `Ruta generada (${adminRouteScope}) con ${nextRoute.orderedPoints.length} puntos.` });
      }
    } catch (error) {
      setMessage(toUiMessage(error, "No se pudo generar la ruta."));
    } finally {
      setLoadingAdminRoute(false);
    }
  };

  const openAdminRouteStartPicker = (): void => {
    setAdminRouteStartPickerOpen(true);
  };

  const confirmAdminRouteStart = async (point: GeoPoint): Promise<void> => {
    setAdminRouteStartPickerOpen(false);
    await generateAdminRoute(point);
  };

  const handleImageChange = (event: ChangeEvent<HTMLInputElement>): void => {
    const nextFile = event.target.files?.[0] ?? null;
    if (!nextFile) {
      return;
    }

    if (previewUrlRef.current) {
      URL.revokeObjectURL(previewUrlRef.current);
      previewUrlRef.current = null;
    }

    const nextPreviewUrl = URL.createObjectURL(nextFile);
    previewUrlRef.current = nextPreviewUrl;
    setSelectedImage(nextFile);
    setPreviewUrl(nextPreviewUrl);
    setMessage({ type: "info", text: "Imagen lista para enviar." });
    event.target.value = "";
  };

  const handleAddressChange = (event: ChangeEvent<HTMLInputElement>): void => {
    const nextAddress = event.target.value;
    setAddress(nextAddress);
    setAddressDirty(true);
    setAddressConfirmed(false);
    setAddressError(null);
    if (!nextAddress.trim()) {
      setAddressSuggestions([]);
    }
  };

  const applyAddressSuggestion = (suggestion: GeocodingSuggestion, successText: string): void => {
    skipCoordinateSyncRef.current = true;
    setLatitude(suggestion.point.lat.toFixed(6));
    setLongitude(suggestion.point.lng.toFixed(6));
    setSelectedPoint(suggestion.point);
    setAddress(suggestion.label);
    setAddressDirty(false);
    setAddressConfirmed(true);
    setAddressSuggestions([]);
    setAddressError(null);
    setMessage({ type: "info", text: successText });
  };

  const handleSelectPoint = (point: GeoPoint): void => {
    applyPoint(point, "Ubicacion actualizada desde el mapa.");
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();

    if (!selectedImage) {
      setMessage({ type: "warning", text: "Selecciona una imagen o saca una foto antes de enviar." });
      return;
    }

    const lat = parseCoordinate(latitude);
    const lng = parseCoordinate(longitude);
    if (lat == null || lng == null) {
      setMessage({ type: "warning", text: "Latitud y longitud deben ser valores numericos validos." });
      return;
    }

    if (!isInsideCordobaCapital({ lat, lng })) {
      setMessage({ type: "warning", text: "La ubicacion debe estar dentro de Cordoba Capital." });
      return;
    }

    if (address.trim() && !addressConfirmed) {
      setMessage({
        type: "warning",
        text: "La direccion escrita aun no fue validada. Selecciona una opcion del listado."
      });
      return;
    }

    setLoadingSubmit(true);
    try {
      await submitReport({
        image: selectedImage,
        user: session.user.email,
        lat,
        lng
      });
      if (isAdmin) {
        await refreshAdminReports(false, adminReportSource);
      }
      setMessage({
        type: "success",
        text: "Reporte recibido. Gestion lo mostrara enseguida; el dashboard solo muestra reportes validados por IA."
      });
    } catch (error) {
      setMessage(toUiMessage(error, "No se pudo enviar el reporte."));
    } finally {
      setLoadingSubmit(false);
    }
  };

  const openAdminPanel = (): void => {
    setAdminPanelOpen(true);
    void refreshAdminReports(false, adminReportSource);
  };

  const routeReportsForMap: ReportDto[] = adminRoute
    ? adminRoute.orderedPoints.map((point) => {
        const fromAdminList = adminReports.find((report) => report.id === point.reportId);
        if (fromAdminList) {
          return fromAdminList;
        }
        return {
          id: point.reportId,
          user: point.user,
          imageUrl: "",
          location: point.coordinate,
          createdAt: point.createdAt,
          status: "PROCESSED_VALID",
          classificationResult: "Pendiente de gestion operativa.",
          isTrash: true
        };
      })
    : [];

  return (
    <div className="app-root">
      <main className="quick-report-shell">
        <header className="quick-report-header">
          <div>
            <AppBrand variant="header" />
            <h1>Nuevo reporte</h1>
            <p>Subi foto y ubicacion de la basura.</p>
          </div>
          <div className="quick-header-actions">
            {isAdmin && (
              <>
                <button type="button" className="session-link admin-link" onClick={() => navigate("/admin/reports")}>
                  Gestion
                </button>
                <button type="button" className="session-link admin-link" onClick={openAdminPanel}>
                  Dashboard
                </button>
              </>
            )}
            <button
              type="button"
              className="session-link"
              onClick={() => {
                logout();
                navigate("/login");
              }}
            >
              Salir
            </button>
          </div>
        </header>

        <StatusBanner message={message} />

        <form className="quick-report-card" onSubmit={handleSubmit}>
          <div className="image-actions">
            <label className="btn btn-secondary file-action-btn">
              Seleccionar imagen
              <input type="file" accept="image/*" onChange={handleImageChange} />
            </label>

            <label className="btn btn-secondary file-action-btn">
              Sacar foto
              <input type="file" accept="image/*" capture="environment" onChange={handleImageChange} />
            </label>
          </div>

          <div className="preview-box quick-preview">
            {previewUrl ? <img src={previewUrl} alt="Preview del reporte" /> : <span>Preview de imagen</span>}
          </div>

          <label className="field-label">
            Direccion
            <input
              type="text"
              value={address}
              onChange={handleAddressChange}
              placeholder="Ej: Av. Colon 1234, Cordoba"
              autoComplete="off"
            />
            {loadingAddressLookup && addressDirty && address.trim().length >= 3 && (
              <span className="address-hint">Buscando sugerencias...</span>
            )}
            {addressError && <span className="address-error">{addressError}</span>}
            {addressSuggestions.length > 0 && (
              <div className="address-suggestions" role="listbox" aria-label="Sugerencias de direccion">
                {addressSuggestions.map((suggestion) => {
                  const key = `${suggestion.point.lat.toFixed(6)}-${suggestion.point.lng.toFixed(6)}`;
                  return (
                    <button
                      key={key}
                      type="button"
                      className="address-suggestion"
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => applyAddressSuggestion(suggestion, "Direccion aplicada a la ubicacion.")}
                    >
                      {suggestion.label}
                    </button>
                  );
                })}
              </div>
            )}
          </label>

          <div className="location-grid">
            <label className="field-label">
              Latitud
              <input
                type="number"
                step="0.000001"
                value={latitude}
                onChange={(event) => setLatitude(event.target.value)}
                required
              />
            </label>

            <label className="field-label">
              Longitud
              <input
                type="number"
                step="0.000001"
                value={longitude}
                onChange={(event) => setLongitude(event.target.value)}
                required
              />
            </label>
          </div>

          <button type="button" className="btn btn-ghost" onClick={() => setMapOpen(true)}>
            Ver mapa
          </button>

          <button type="submit" className="btn btn-primary" disabled={loadingSubmit || !selectedImage}>
            {loadingSubmit ? "Enviando..." : "Enviar reporte"}
          </button>
        </form>
      </main>

      <LocationPickerOverlay
        open={mapOpen}
        selectedPoint={selectedPoint}
        onSelectPoint={handleSelectPoint}
        onClose={() => setMapOpen(false)}
      />

      {isAdmin && (
        <>
          <AdminControlPanel
            open={adminPanelOpen}
            reportSource={adminReportSource}
            routeScope={adminRouteScope}
            reportCount={adminReports.length}
            routePoints={adminRoute?.orderedPoints.length ?? 0}
            totalDistanceKm={adminRoute?.totalDistanceKm ?? 0}
            loadingReports={loadingAdminReports}
            loadingRoute={loadingAdminRoute}
            onClose={() => setAdminPanelOpen(false)}
            onReportSourceChange={(value) => {
              setAdminReportSource(value);
              void refreshAdminReports(false, value);
            }}
            onRouteScopeChange={setAdminRouteScope}
            onRefreshReports={() => refreshAdminReports(true)}
            onGenerateRoute={async () => {
              openAdminRouteStartPicker();
            }}
          />

          <MapOverlay
            open={adminMapOpen}
            reports={routeReportsForMap}
            route={adminRoute}
            reportSource={adminReportSource}
            onClose={() => setAdminMapOpen(false)}
          />
        </>
      )}

      {isAdmin && (
        <LocationPickerOverlay
          open={adminRouteStartPickerOpen}
          selectedPoint={adminRouteStartPoint}
          onSelectPoint={setAdminRouteStartPoint}
          onClose={() => setAdminRouteStartPickerOpen(false)}
          title="Ubicacion actual del recolector"
          subtitle="Selecciona el punto de inicio dentro de Cordoba Capital para generar la ruta."
          confirmLabel="Confirmar ubicacion y generar ruta"
          onConfirm={(point) => {
            void confirmAdminRouteStart(point);
          }}
          loadingConfirm={loadingAdminRoute}
        />
      )}
    </div>
  );
}
