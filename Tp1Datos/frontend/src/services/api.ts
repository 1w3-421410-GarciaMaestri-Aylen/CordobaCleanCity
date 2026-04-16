import type {
  AdminReportDto,
  AdminReportStatus,
  OptimalRouteResponse,
  RouteCoordinate,
  ReportDto,
  ReportSubmissionResponse,
  RouteScope,
  UploadReportPayload
} from "../types/api";
import { httpRequest } from "./httpClient";

export async function submitReport(payload: UploadReportPayload): Promise<ReportSubmissionResponse> {
  const formData = new FormData();
  formData.append("image", payload.image);
  formData.append("user", payload.user);
  formData.append("lat", String(payload.lat));
  formData.append("lng", String(payload.lng));

  return httpRequest<ReportSubmissionResponse>("/api/reports", {
    method: "POST",
    body: formData
  });
}

export async function fetchAllReports(): Promise<ReportDto[]> {
  return httpRequest<ReportDto[]>("/api/reports");
}

export async function fetchTodayReports(): Promise<ReportDto[]> {
  return httpRequest<ReportDto[]>("/api/reports/today");
}

export async function fetchOptimalRoute(scope: RouteScope, start: RouteCoordinate): Promise<OptimalRouteResponse> {
  const params = new URLSearchParams({
    scope,
    startLat: String(start.lat),
    startLng: String(start.lng)
  });
  return httpRequest<OptimalRouteResponse>(`/api/routes/optimal?${params.toString()}`);
}

export async function fetchAdminReports(): Promise<AdminReportDto[]> {
  return httpRequest<AdminReportDto[]>("/api/admin/reports");
}

export async function updateAdminReportStatus(
  reportId: string,
  status: AdminReportStatus
): Promise<AdminReportDto> {
  return httpRequest<AdminReportDto>(`/api/admin/reports/${reportId}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status })
  });
}
