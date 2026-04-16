import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute, PublicOnlyRoute } from "./components/AuthRoute";
import { AdminReportsPage } from "./pages/AdminReportsPage";
import { AdminUsersPage } from "./pages/AdminUsersPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ReportWorkspacePage } from "./pages/ReportWorkspacePage";
import { VerifyEmailPage } from "./pages/VerifyEmailPage";
import { VerifyPendingPage } from "./pages/VerifyPendingPage";

export function App(): JSX.Element {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/app" replace />} />

      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      <Route path="/verify-pending" element={<VerifyPendingPage />} />
      <Route path="/verify-email/confirm" element={<VerifyEmailPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/app" element={<ReportWorkspacePage />} />
      </Route>

      <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
        <Route path="/admin/reports" element={<AdminReportsPage />} />
        <Route path="/admin/users" element={<AdminUsersPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  );
}
