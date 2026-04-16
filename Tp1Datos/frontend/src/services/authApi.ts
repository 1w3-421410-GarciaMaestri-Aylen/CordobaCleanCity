import type {
  AuthUser,
  EmailVerificationResponse,
  LoginRequestPayload,
  LoginResponse,
  RegisterRequestPayload,
  RegisterResponse
} from "../types/api";
import { httpRequest } from "./httpClient";

export async function registerUser(payload: RegisterRequestPayload): Promise<RegisterResponse> {
  return httpRequest<RegisterResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function loginUser(payload: LoginRequestPayload): Promise<LoginResponse> {
  return httpRequest<LoginResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function fetchCurrentUser(): Promise<AuthUser> {
  return httpRequest<AuthUser>("/api/auth/me");
}

export async function verifyEmailToken(token: string): Promise<EmailVerificationResponse> {
  const encodedToken = encodeURIComponent(token);
  return httpRequest<EmailVerificationResponse>(`/api/auth/verify-email?token=${encodedToken}`);
}

export async function resendVerificationEmail(email: string): Promise<EmailVerificationResponse> {
  return httpRequest<EmailVerificationResponse>("/api/auth/verify-email/resend", {
    method: "POST",
    body: JSON.stringify({ email })
  });
}

export async function fetchAdminUsers(): Promise<AuthUser[]> {
  return httpRequest<AuthUser[]>("/api/admin/users");
}
