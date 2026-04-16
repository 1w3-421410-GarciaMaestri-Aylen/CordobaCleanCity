import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { AppBrand } from "../components/AppBrand";
import { ApiError } from "../services/httpClient";

export function LoginPage(): JSX.Element {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      await login({ email, password });
      navigate("/app");
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        setError("Tu email aun no esta verificado. Revisa tu correo o solicita un nuevo enlace.");
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError("No se pudo iniciar sesion.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-root">
      <section className="auth-card">
        <AppBrand variant="auth" />
        <h1>Iniciar sesion</h1>
        <p className="auth-subtitle">Accede para reportar puntos de basura y gestionar rutas.</p>

        {error && <div className="status-banner status-error"><strong>Error:</strong> {error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <label className="field-label">
            Email
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          </label>

          <label className="field-label">
            Contraseña
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
          </label>

          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? "Ingresando..." : "Ingresar"}
          </button>
        </form>

        <div className="auth-links">
          <Link to="/register">Crear cuenta</Link>
          <Link to={`/verify-pending?email=${encodeURIComponent(email)}`}>Email sin verificar</Link>
        </div>
      </section>
    </div>
  );
}
