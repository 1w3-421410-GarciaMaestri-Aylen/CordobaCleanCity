import L from "leaflet";
import type { ReportDto } from "../types/api";
import { buildReportImageFallbackDataUri, resolveReportImageUrl } from "../utils/reportImages";

export function createReportMarkerIcon(report: ReportDto): L.DivIcon {
  const imageSrc = report.imageUrl ? resolveReportImageUrl(report.imageUrl) : "";
  const fallbackSrc = buildReportImageFallbackDataUri();
  const imageHtml = imageSrc
    ? `<img src="${imageSrc}" alt="Reporte ${report.id}" style="width:100%;height:100%;object-fit:cover;display:block;" onerror="this.onerror=null;this.src='${fallbackSrc}'" />`
    : `<div style="width:100%;height:100%;background:#cbd5e1;"></div>`;

  return L.divIcon({
    className: "report-thumbnail-marker",
    html: `
      <div style="display:flex;flex-direction:column;align-items:center;transform:translateY(-10px);">
        <div style="width:56px;height:56px;border-radius:10px;overflow:hidden;border:2px solid #ffffff;box-shadow:0 8px 18px rgba(15,23,42,0.28);background:#e2e8f0;">
          ${imageHtml}
        </div>
        <div style="width:14px;height:14px;margin-top:6px;border-radius:999px;background:#1f7f74;border:3px solid #ffffff;box-shadow:0 4px 12px rgba(15,23,42,0.24);"></div>
      </div>
    `,
    iconSize: [56, 76],
    iconAnchor: [28, 76],
    popupAnchor: [0, -76]
  });
}

export function createSelectedLocationIcon(): L.DivIcon {
  return L.divIcon({
    className: "selected-location-marker",
    html: `
      <div style="display:flex;flex-direction:column;align-items:center;transform:translateY(-8px);">
        <div style="width:22px;height:22px;border-radius:999px;background:#1f7f74;border:4px solid #ffffff;box-shadow:0 8px 18px rgba(15,23,42,0.28);"></div>
        <div style="width:8px;height:8px;margin-top:4px;border-radius:999px;background:#134e4a;box-shadow:0 2px 6px rgba(15,23,42,0.18);"></div>
      </div>
    `,
    iconSize: [22, 34],
    iconAnchor: [11, 34],
    popupAnchor: [0, -34]
  });
}

export function createRouteStartIcon(): L.DivIcon {
  return L.divIcon({
    className: "route-start-marker",
    html: `
      <div style="display:flex;flex-direction:column;align-items:center;transform:translateY(-8px);">
        <div style="width:24px;height:24px;border-radius:999px;background:#1d4ed8;border:4px solid #ffffff;box-shadow:0 8px 18px rgba(15,23,42,0.28);"></div>
        <div style="width:10px;height:10px;margin-top:4px;border-radius:999px;background:#1e3a8a;box-shadow:0 2px 6px rgba(15,23,42,0.18);"></div>
      </div>
    `,
    iconSize: [24, 36],
    iconAnchor: [12, 36],
    popupAnchor: [0, -36]
  });
}

export function resolveImageUrl(imageUrl: string): string {
  return resolveReportImageUrl(imageUrl);
}
