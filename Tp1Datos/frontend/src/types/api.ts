export type MessageType = "success" | "info" | "warning" | "error";
export type ReportSource = "today" | "all";
export type UserRole = "USER" | "ADMIN";
export type AdminReportStatus = "PENDING" | "RESOLVED";

export interface UiMessage {
  type: MessageType;
  text: string;
}

export interface ReportLocation {
  lat: number;
  lng: number;
}

export interface ReportDto {
  id: string;
  user: string;
  imageUrl: string;
  location: ReportLocation;
  createdAt: string;
  status: string;
  classificationResult: string;
  isTrash: boolean;
}

export interface AdminReportDto {
  id: string;
  requestId: string;
  userId: string;
  userEmail: string;
  user: string;
  imageUrl: string;
  location: ReportLocation;
  createdAt: string;
  status: string;
  adminStatus: AdminReportStatus;
  adminStatusUpdatedAt: string | null;
  classificationResult: string;
  isTrash: boolean;
}

export interface ReportSubmissionResponse {
  requestId: string;
  status: string;
  message: string;
  imageUrl: string;
}

export type RouteScope = "today" | "active";

export interface RouteCoordinate {
  lat: number;
  lng: number;
}

export interface RouteOrderedPoint {
  sequence: number;
  reportId: string;
  user: string;
  coordinate: RouteCoordinate;
  createdAt: string;
}

export interface RouteMetadata {
  algorithm: string;
  scope: string;
  sourceReportCount: number;
  routingProvider: string;
  routingStatus: string;
  routingMessage: string | null;
}

export interface OptimalRouteResponse {
  orderedPoints: RouteOrderedPoint[];
  startCoordinate: RouteCoordinate | null;
  coordinates: RouteCoordinate[];
  geometry: RouteCoordinate[];
  totalDistanceKm: number;
  metadata: RouteMetadata;
  generatedAt: string;
}

export interface UploadReportPayload {
  image: File;
  user: string;
  lat: number;
  lng: number;
}

export interface RegisterRequestPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface RegisterResponse {
  email: string;
  verificationRequired: boolean;
  message: string;
}

export interface LoginRequestPayload {
  email: string;
  password: string;
}

export interface AuthUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: UserRole;
  emailVerified: boolean;
}

export interface LoginResponse {
  tokenType: string;
  accessToken: string;
  expiresAt: string;
  user: AuthUser;
}

export interface EmailVerificationResponse {
  verified: boolean;
  message: string;
}

export interface AuthSession {
  accessToken: string;
  expiresAt: string;
  user: AuthUser;
}
