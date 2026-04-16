import { createContext, useContext, useEffect, useRef, useState } from "react";
import type { ReactNode } from "react";
import {
  fetchCurrentUser,
  loginUser,
  registerUser,
  resendVerificationEmail
} from "../services/authApi";
import { configureHttpClient } from "../services/httpClient";
import type {
  AuthSession,
  EmailVerificationResponse,
  LoginRequestPayload,
  RegisterRequestPayload,
  RegisterResponse
} from "../types/api";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  status: AuthStatus;
  session: AuthSession | null;
  login: (payload: LoginRequestPayload) => Promise<void>;
  register: (payload: RegisterRequestPayload) => Promise<RegisterResponse>;
  resendVerification: (email: string) => Promise<EmailVerificationResponse>;
  refreshCurrentUser: () => Promise<void>;
  logout: () => void;
}

const SESSION_STORAGE_KEY = "ecoruta.auth.session";
const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredSession(): AuthSession | null {
  try {
    const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    return JSON.parse(raw) as AuthSession;
  } catch {
    return null;
  }
}

function persistSession(session: AuthSession | null): void {
  if (!session) {
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
    return;
  }
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
}

function isSessionExpired(session: AuthSession): boolean {
  return new Date(session.expiresAt).getTime() <= Date.now();
}

export function AuthProvider({ children }: { children: ReactNode }): JSX.Element {
  const initialSession = readStoredSession();
  const [status, setStatus] = useState<AuthStatus>(initialSession ? "loading" : "unauthenticated");
  const [session, setSession] = useState<AuthSession | null>(initialSession);
  const accessTokenRef = useRef<string | null>(initialSession?.accessToken ?? null);

  const clearSession = (): void => {
    accessTokenRef.current = null;
    setSession(null);
    setStatus("unauthenticated");
    persistSession(null);
  };

  useEffect(() => {
    configureHttpClient({
      getAccessToken: () => accessTokenRef.current,
      onUnauthorized: clearSession
    });
  }, []);

  useEffect(() => {
    accessTokenRef.current = session?.accessToken ?? null;
  }, [session]);

  useEffect(() => {
    if (!initialSession) {
      return;
    }
    if (isSessionExpired(initialSession)) {
      clearSession();
      return;
    }

    let active = true;
    const bootstrap = async (): Promise<void> => {
      try {
        const user = await fetchCurrentUser();
        if (!active) {
          return;
        }
        const nextSession: AuthSession = {
          ...initialSession,
          user
        };
        setSession(nextSession);
        setStatus("authenticated");
        persistSession(nextSession);
      } catch {
        if (!active) {
          return;
        }
        clearSession();
      }
    };

    void bootstrap();
    return () => {
      active = false;
    };
  }, []);

  const login = async (payload: LoginRequestPayload): Promise<void> => {
    const response = await loginUser(payload);
    const nextSession: AuthSession = {
      accessToken: response.accessToken,
      expiresAt: response.expiresAt,
      user: response.user
    };
    accessTokenRef.current = nextSession.accessToken;
    setSession(nextSession);
    setStatus("authenticated");
    persistSession(nextSession);
  };

  const register = async (payload: RegisterRequestPayload): Promise<RegisterResponse> => {
    return registerUser(payload);
  };

  const resendVerification = async (email: string): Promise<EmailVerificationResponse> => {
    return resendVerificationEmail(email);
  };

  const refreshCurrentUser = async (): Promise<void> => {
    if (!session) {
      clearSession();
      return;
    }
    const user = await fetchCurrentUser();
    const nextSession = {
      ...session,
      user
    };
    setSession(nextSession);
    persistSession(nextSession);
  };

  const value: AuthContextValue = {
    status,
    session,
    login,
    register,
    resendVerification,
    refreshCurrentUser,
    logout: clearSession
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
