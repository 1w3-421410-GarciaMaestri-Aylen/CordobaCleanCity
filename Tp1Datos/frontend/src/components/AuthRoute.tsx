import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import type { UserRole } from "../types/api";

interface ProtectedRouteProps {
  roles?: UserRole[];
}

export function ProtectedRoute({ roles }: ProtectedRouteProps): JSX.Element {
  const { status, session } = useAuth();

  if (status === "loading") {
    return <div className="auth-loading">Cargando sesion...</div>;
  }

  if (status !== "authenticated" || !session) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !roles.includes(session.user.role)) {
    return <Navigate to="/app" replace />;
  }

  return <Outlet />;
}

export function PublicOnlyRoute(): JSX.Element {
  const { status } = useAuth();
  if (status === "loading") {
    return <div className="auth-loading">Cargando sesion...</div>;
  }
  if (status === "authenticated") {
    return <Navigate to="/app" replace />;
  }
  return <Outlet />;
}
