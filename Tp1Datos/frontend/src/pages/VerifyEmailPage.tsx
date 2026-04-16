import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { AppBrand } from "../components/AppBrand";
import { verifyEmailToken } from "../services/authApi";
import { ApiError } from "../services/httpClient";

type VerificationState = "checking" | "success" | "error";

export function VerifyEmailPage(): JSX.Element {
  const [searchParams] = useSearchParams();
  const [state, setState] = useState<VerificationState>("checking");
  const [message, setMessage] = useState("Validando token de verificacion...");

  useEffect(() => {
    const token = searchParams.get("token");
    if (!token) {
      setState("error");
      setMessage("Token de verificacion faltante.");
      return;
    }

    let active = true;
    const verify = async (): Promise<void> => {
      try {
        const response = await verifyEmailToken(token);
        if (!active) {
          return;
        }
        setState(response.verified ? "success" : "error");
        setMessage(response.message);
      } catch (err) {
        if (!active) {
          return;
        }
        if (err instanceof ApiError || err instanceof Error) {
          setMessage(err.message);
        } else {
          setMessage("No se pudo verificar el email.");
        }
        setState("error");
      }
    };

    void verify();
    return () => {
      active = false;
    };
  }, [searchParams]);

  return (
    <div className="auth-root">
      <section className="auth-card">
        <AppBrand variant="auth" />
        <h1>Verificacion de email</h1>
        <p className="auth-subtitle">{message}</p>

        {state === "checking" && <div className="status-banner status-info"><strong>Info:</strong> Procesando...</div>}
        {state === "success" && <div className="status-banner status-success"><strong>Listo:</strong> Cuenta verificada.</div>}
        {state === "error" && <div className="status-banner status-error"><strong>Error:</strong> Verificacion fallida.</div>}

        <div className="auth-links">
          <Link to="/login">Ir a login</Link>
          <Link to="/verify-pending">Reenviar verificacion</Link>
        </div>
      </section>
    </div>
  );
}
