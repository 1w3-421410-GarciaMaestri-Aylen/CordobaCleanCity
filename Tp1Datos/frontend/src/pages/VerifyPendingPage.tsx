import { useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { AppBrand } from "../components/AppBrand";
import { ApiError } from "../services/httpClient";

export function VerifyPendingPage(): JSX.Element {
  const [searchParams] = useSearchParams();
  const initialEmail = useMemo(() => searchParams.get("email") ?? "", [searchParams]);
  const { resendVerification } = useAuth();
  const [email, setEmail] = useState(initialEmail);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleResend = async (): Promise<void> => {
    if (!email.trim()) {
      setError("Ingresa un email valido.");
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");
    try {
      const response = await resendVerification(email);
      setMessage(response.message);
    } catch (err) {
      if (err instanceof ApiError || err instanceof Error) {
        setError(err.message);
      } else {
        setError("No se pudo reenviar el email de verificacion.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-root">
      <section className="auth-card">
        <AppBrand variant="auth" />
        <h1>Email pendiente</h1>
        <p className="auth-subtitle">
          Tu cuenta existe, pero necesitas verificar el email antes de usar la aplicacion.
        </p>

        {message && <div className="status-banner status-info"><strong>Info:</strong> {message}</div>}
        {error && <div className="status-banner status-error"><strong>Error:</strong> {error}</div>}

        <div className="auth-form">
          <label className="field-label">
            Email de registro
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          </label>
          <button type="button" className="btn btn-secondary" onClick={() => void handleResend()} disabled={loading}>
            {loading ? "Enviando..." : "Reenviar verificacion"}
          </button>
        </div>

        <div className="auth-links">
          <Link to="/login">Volver a login</Link>
        </div>
      </section>
    </div>
  );
}
