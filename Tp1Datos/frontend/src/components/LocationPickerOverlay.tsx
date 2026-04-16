import { useEffect } from "react";
import L from "leaflet";
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from "react-leaflet";
import type { LeafletEventHandlerFnMap } from "leaflet";
import { createSelectedLocationIcon } from "./mapMarkerIcons";
import {
  type GeoPoint,
  isInsideCordobaCapital
} from "../constants/location";
import {
  CORDOBA_DEFAULT_ZOOM,
  CORDOBA_MAP_BOUNDS,
  CORDOBA_MAP_CENTER,
  CORDOBA_MAX_ZOOM,
  CORDOBA_MIN_ZOOM,
  CORDOBA_SELECTED_POINT_ZOOM
} from "../constants/mapConfig";

L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png"
});

interface MapBehaviorProps {
  selectedPoint: GeoPoint | null;
  onSelectPoint: (point: GeoPoint) => void;
}

interface LocationPickerOverlayProps {
  open: boolean;
  selectedPoint: GeoPoint | null;
  onSelectPoint: (point: GeoPoint) => void;
  onClose: () => void;
  title?: string;
  subtitle?: string;
  confirmLabel?: string;
  onConfirm?: (point: GeoPoint) => void;
  loadingConfirm?: boolean;
}

function MapBehavior({ selectedPoint, onSelectPoint }: MapBehaviorProps): null {
  const map = useMap();

  useEffect(() => {
    map.setMaxBounds(CORDOBA_MAP_BOUNDS);
    map.setView(
      selectedPoint ? [selectedPoint.lat, selectedPoint.lng] : CORDOBA_MAP_CENTER,
      selectedPoint ? CORDOBA_SELECTED_POINT_ZOOM : CORDOBA_DEFAULT_ZOOM
    );
  }, [map, selectedPoint]);

  useMapEvents({
    click(event) {
      const point = {
        lat: Number(event.latlng.lat.toFixed(6)),
        lng: Number(event.latlng.lng.toFixed(6))
      };
      if (isInsideCordobaCapital(point)) {
        onSelectPoint(point);
      }
    }
  });

  return null;
}

export function LocationPickerOverlay({
  open,
  selectedPoint,
  onSelectPoint,
  onClose,
  title = "Elegir ubicacion",
  subtitle = "Toca un punto dentro de Cordoba Capital",
  confirmLabel = "Confirmar ubicacion",
  onConfirm,
  loadingConfirm = false
}: LocationPickerOverlayProps): JSX.Element | null {
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

  const markerHandlers: LeafletEventHandlerFnMap | undefined = selectedPoint
    ? {
        dragend(event) {
          const marker = event.target as L.Marker;
          const nextPoint = marker.getLatLng();
          const point = {
            lat: Number(nextPoint.lat.toFixed(6)),
            lng: Number(nextPoint.lng.toFixed(6))
          };
          if (isInsideCordobaCapital(point)) {
            onSelectPoint(point);
          } else {
            marker.setLatLng([selectedPoint.lat, selectedPoint.lng]);
          }
        }
      }
    : undefined;

  return (
    <div className="location-overlay-backdrop" role="presentation">
      <section className="location-overlay" role="dialog" aria-modal="true" aria-label="Seleccion de ubicacion">
        <header className="location-overlay-header">
          <div>
            <h2>{title}</h2>
            <p>{subtitle}</p>
          </div>
          <button type="button" className="btn btn-outline compact-btn" onClick={onClose}>
            Cerrar
          </button>
        </header>

        <div className="location-selected">
          {selectedPoint
            ? `Lat ${selectedPoint.lat.toFixed(6)} - Lng ${selectedPoint.lng.toFixed(6)}`
            : "No seleccionaste un punto todavia"}
        </div>

        <div className="location-map-wrap">
          <MapContainer
            center={CORDOBA_MAP_CENTER}
            zoom={CORDOBA_DEFAULT_ZOOM}
            minZoom={CORDOBA_MIN_ZOOM}
            maxZoom={CORDOBA_MAX_ZOOM}
            maxBounds={CORDOBA_MAP_BOUNDS}
            maxBoundsViscosity={1}
            className="location-map"
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <MapBehavior selectedPoint={selectedPoint} onSelectPoint={onSelectPoint} />
            {selectedPoint && (
              <Marker
                position={[selectedPoint.lat, selectedPoint.lng]}
                icon={createSelectedLocationIcon()}
                draggable
                eventHandlers={markerHandlers}
              />
            )}
          </MapContainer>
        </div>

        {onConfirm && (
          <button
            type="button"
            className="btn btn-primary location-confirm-btn"
            disabled={!selectedPoint || loadingConfirm}
            onClick={() => {
              if (selectedPoint) {
                onConfirm(selectedPoint);
              }
            }}
          >
            {loadingConfirm ? "Generando..." : confirmLabel}
          </button>
        )}
      </section>
    </div>
  );
}
