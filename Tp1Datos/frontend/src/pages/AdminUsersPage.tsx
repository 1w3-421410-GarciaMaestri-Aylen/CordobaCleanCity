import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { AppBrand } from "../components/AppBrand";
import { fetchAdminUsers } from "../services/authApi";
import { ApiError } from "../services/httpClient";
import type { AuthUser } from "../types/api";

export function AdminUsersPage(): JSX.Element {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [users, setUsers] = useState<AuthUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const loadUsers = async (): Promise<void> => {
      setLoading(true);
      try {
        const nextUsers = await fetchAdminUsers();
        if (!active) {
          return;
        }
        setUsers(nextUsers);
      } catch (err) {
        if (!active) {
          return;
        }
        if (err instanceof ApiError || err instanceof Error) {
          setError(err.message);
        } else {
          setError("No se pudo cargar la lista de usuarios.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    void loadUsers();
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="auth-root">
      <section className="auth-card admin-card">
        <header className="workspace-header">
          <div>
            <AppBrand variant="admin" />
            <h1>Usuarios registrados</h1>
          </div>
          <div className="workspace-actions">
            <button type="button" className="btn btn-secondary compact-btn" onClick={() => navigate("/admin/reports")}>
              Reportes
            </button>
            <button type="button" className="btn btn-outline compact-btn" onClick={() => navigate("/app")}>
              Volver
            </button>
            <button
              type="button"
              className="btn btn-ghost compact-btn"
              onClick={() => {
                logout();
                navigate("/login");
              }}
            >
              Salir
            </button>
          </div>
        </header>

        {loading && <div className="status-banner status-info"><strong>Info:</strong> Cargando usuarios...</div>}
        {error && <div className="status-banner status-error"><strong>Error:</strong> {error}</div>}

        {!loading && !error && (
          <div className="admin-list">
            {users.map((user) => (
              <article key={user.id} className="admin-user-item">
                <h3>{user.firstName} {user.lastName}</h3>
                <p>{user.email}</p>
                <div className="admin-user-meta">
                  <span>{user.role}</span>
                  <span>{user.emailVerified ? "Verificado" : "No verificado"}</span>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
