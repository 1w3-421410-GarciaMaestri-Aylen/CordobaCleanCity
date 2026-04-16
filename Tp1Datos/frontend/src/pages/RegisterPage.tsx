import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { AppBrand } from "../components/AppBrand";
import { ApiError } from "../services/httpClient";

export function RegisterPage(): JSX.Element {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    setError("");

    if (password !== confirmPassword) {
      setError("Las contraseñas no coinciden.");
      return;
    }

    setLoading(true);
    try {
      await register({
        firstName,
        lastName,
        email,
        password
      });
      navigate(`/verify-pending?email=${encodeURIComponent(email)}`);
    } catch (err) {
      if (err instanceof ApiError || err instanceof Error) {
        setError(err.message);
      } else {
        setError("No se pudo completar el registro.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-root">
      <section className="auth-card">
        <AppBrand variant="auth" />
        <h1>Crear cuenta</h1>
        <p className="auth-subtitle">Registrate para acceder al panel privado de reportes.</p>

        {error && <div className="status-banner status-error"><strong>Error:</strong> {error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <label className="field-label">
            Nombre
            <input type="text" value={firstName} onChange={(event) => setFirstName(event.target.value)} required />
          </label>

          <label className="field-label">
            Apellido
            <input type="text" value={lastName} onChange={(event) => setLastName(event.target.value)} required />
          </label>

          <label className="field-label">
            Email
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          </label>

          <label className="field-label">
            Contraseña
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
          </label>

          <label className="field-label">
            Confirmar contraseña
            <input
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              required
            />
          </label>

          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? "Registrando..." : "Registrarme"}
          </button>
        </form>

        <div className="auth-links">
          <Link to="/login">Ya tengo cuenta</Link>
        </div>
      </section>
    </div>
  );
}
