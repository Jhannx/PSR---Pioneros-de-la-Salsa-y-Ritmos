import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { DataProvider } from "./context/DataContext";
import ProtectedRoute from "./components/auth/ProtectedRoute";
import AppLayout from "./layouts/AppLayout";

// Páginas de autenticación y públicas
import LoginPage from "./pages/auth/LoginPage";
import ForgotPasswordPage from "./pages/auth/ForgotPasswordPage";
import ResetPasswordPage from "./pages/auth/ResetPasswordPage";
import SelfRegistrationPage from "./pages/public/SelfRegistrationPage";

// Páginas de la Terminal PSR / Entrada
import LobbyCheckinPage from "./pages/lobby/LobbyCheckinPage";
import LobbyRegistrationPage from "./pages/lobby/LobbyRegistrationPage";
import LobbyQueueDisplayPage from "./pages/lobby/LobbyQueueDisplayPage";
import LobbyPanelPage from "./pages/lobby/LobbyPanelPage";

// Páginas de Recepción y Administración
import ReceptionCheckinPage from "./pages/reception/ReceptionCheckinPage";
import PeopleListPage from "./pages/reception/PeopleListPage";

import SessionsPage from "./pages/admin/SessionsPage";
import SchedulePage from "./pages/admin/SchedulePage";
import ReportsPage from "./pages/admin/ReportsPage";
import AuditPage from "./pages/admin/AuditPage";
import SettingsPage from "./pages/admin/SettingsPage";
import RolesPage from "./pages/admin/RolesPage";

// Páginas de Miembro y Perfil
import MemberHomePage from "./pages/member/MemberHomePage";
import MemberEmergencyContactPage from "./pages/member/MemberEmergencyContactPage";
import MemberRegistrationPage from "./pages/member/MemberRegistrationPage";
import UserProfilePage from "./pages/member/UserProfilePage";

function RoleRedirect() {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user.roles.includes("ADMIN")) {
    return <Navigate to="/admin/reports" replace />;
  }
  if (user.roles.includes("RECEPTIONIST") || user.roles.includes("TEACHER")) {
    return <Navigate to="/reception/checkin" replace />;
  }
  if (user.roles.includes("PSR_TERMINAL")) {
    return <Navigate to="/lobby/panel" replace />;
  }
  return <Navigate to="/member/home" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <DataProvider>
          <Routes>
            {/* Rutas Públicas */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/restablecer-password" element={<ResetPasswordPage />} />
            <Route path="/register" element={<SelfRegistrationPage />} />
            <Route path="/member/register" element={<MemberRegistrationPage />} />

            {/* Rutas de la Terminal PSR / Pantalla de Entrada */}
            <Route path="/lobby/checkin" element={<LobbyCheckinPage />} />
            <Route path="/lobby/register" element={<LobbyRegistrationPage />} />
            <Route path="/lobby/queue" element={<LobbyQueueDisplayPage />} />

            {/* Rutas Protegidas dentro de la Estructura AppLayout */}
            <Route
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/" element={<RoleRedirect />} />

              {/* Ruta Mi Perfil, neutral (sin prefijo de rol), accesible por cualquier usuario autenticado */}
              <Route path="/profile" element={<UserProfilePage />} />

              {/* Panel de la Terminal PSR (con sidebar) — desde aquí se elige la pantalla a usar */}
              <Route
                path="/lobby/panel"
                element={
                  <ProtectedRoute allowedRoles={["PSR_TERMINAL"]}>
                    <LobbyPanelPage />
                  </ProtectedRoute>
                }
              />

              {/* Rutas de Recepción */}
              <Route
                path="/reception/checkin"
                element={
                  <ProtectedRoute allowedRoles={["ADMIN", "RECEPTIONIST", "TEACHER"]}>
                    <ReceptionCheckinPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/reception/people"
                element={
                  <ProtectedRoute allowedRoles={["RECEPTIONIST", "TEACHER"]}>
                    <PeopleListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/reception/sessions"
                element={
                  <ProtectedRoute allowedRoles={["RECEPTIONIST", "TEACHER"]}>
                    <SessionsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/reception/schedule"
                element={
                  <ProtectedRoute allowedRoles={["RECEPTIONIST", "TEACHER"]}>
                    <SchedulePage />
                  </ProtectedRoute>
                }
              />
              {/* Tarifa/métodos de pago/config de cola en solo lectura para RECEPCIÓN y PROFESOR
                  (PENDIENTES.md: ambos ven, solo ADMIN modifica; métodos de pago además solo lo ve
                  RECEPCIÓN, filtro que resuelve SettingsPage internamente). */}
              <Route
                path="/reception/settings"
                element={
                  <ProtectedRoute allowedRoles={["RECEPTIONIST", "TEACHER"]}>
                    <SettingsPage />
                  </ProtectedRoute>
                }
              />
              {/* Reportes de ingresos: la matriz solo se los da a RECEPCIÓN, no a PROFESOR. */}
              <Route
                path="/reception/reports"
                element={
                  <ProtectedRoute allowedRoles={["RECEPTIONIST"]}>
                    <ReportsPage />
                  </ProtectedRoute>
                }
              />

              {/* Rutas de Administración */}
              <Route
                path="/admin/people"
                element={
                  <ProtectedRoute allowedRoles={["ADMIN"]}>
                    <PeopleListPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/reports"
                element={
                  <ProtectedRoute allowedRoles={["ADMIN"]}>
                    <ReportsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/sessions"
                element={
                  <ProtectedRoute allowedRoles={["ADMIN"]}>
                    <SessionsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/schedule"
                element={
                  <ProtectedRoute allowedRoles={["ADMIN"]}>
                    <SchedulePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/audit"
                element={
                  <ProtectedRoute allowedRoles={["ADMIN"]}>
                    <AuditPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/roles"
                element={
                  <ProtectedRoute allowedRoles={["ADMIN"]}>
                    <RolesPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/settings"
                element={
                  <ProtectedRoute allowedRoles={["ADMIN"]}>
                    <SettingsPage />
                  </ProtectedRoute>
                }
              />

              {/* Rutas de Miembro */}
              <Route
                path="/member/home"
                element={
                  <ProtectedRoute allowedRoles={["ADMIN", "MEMBER"]}>
                    <MemberHomePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/member/emergency-contact"
                element={
                  <ProtectedRoute allowedRoles={["ADMIN", "MEMBER", "TEACHER", "RECEPTIONIST"]}>
                    <MemberEmergencyContactPage />
                  </ProtectedRoute>
                }
              />
            </Route>

            {/* Ruta por defecto */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </DataProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
