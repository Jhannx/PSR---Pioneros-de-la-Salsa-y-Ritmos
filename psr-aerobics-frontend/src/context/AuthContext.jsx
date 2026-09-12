/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect } from "react";
import { authService } from "../services/authService";
import {
  TOKEN_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
  REFRESH_ENABLED,
  storeExpiry,
  scheduleProactiveRefresh,
  cancelProactiveRefresh,
} from "../services/apiClient";

const AUTH_STORAGE_KEY = "psr_aerobics_user_session_v2";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem(AUTH_STORAGE_KEY);
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  useEffect(() => {
    try {
      if (user) {
        localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
      } else {
        localStorage.removeItem(AUTH_STORAGE_KEY);
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
        // Cubre también la expulsión por "psr:unauthorized" (refresh token vencido/revocado),
        // que pasa por acá y no por logout() — sin esto quedaría un timer programado apuntando
        // a una sesión que ya no existe.
        cancelProactiveRefresh();
      }
    } catch (e) {
      console.error("Error al guardar la sesión de usuario:", e);
    }
  }, [user]);

  useEffect(() => {
    const clearExpiredSession = () => setUser(null);
    window.addEventListener("psr:unauthorized", clearExpiredSession);
    return () => window.removeEventListener("psr:unauthorized", clearExpiredSession);
  }, []);

  // Si ya había sesión guardada (recarga de página, pestaña nueva), retoma la renovación
  // proactiva en vez de esperar a que la primera petición real dé 401. No hace nada si
  // REFRESH_ENABLED está apagado o si no hay expiración guardada de una sesión anterior.
  useEffect(() => {
    if (user) scheduleProactiveRefresh();
  }, [user]);

  async function login(identifier, password) {
    try {
      const response = await authService.login(identifier, password);
      if (response && response.token && response.user) {
        localStorage.setItem(TOKEN_STORAGE_KEY, response.token);
        // Sesión persistente (docs/PLAN-IMPLEMENTACION.md Fase 4, detrás de
        // VITE_AUTH_REFRESH_ENABLED) — se guarda si el backend ya lo manda en la respuesta de
        // login; mientras no lo mande, esto simplemente no ocurre y nada más se activa.
        if (response.refreshToken) {
          localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, response.refreshToken);
        }
        // `expiresIn` (segundos) habilita renovar por reloj en vez de esperar un 401
        // (NOTAS-PARA-FRONTEND.md §2) — se guarda y se programa el timer de una vez.
        storeExpiry(response.expiresIn);
        scheduleProactiveRefresh();
        const userData = {
          userId: response.user.userId,
          personId: response.user.personId,
          documentNumber: response.user.documentNumber,
          fullName: response.user.fullName || response.user.name || "Usuario",
          email: response.user.email,
          roles: response.user.roles || (response.user.role ? [response.user.role] : []),
        };
        setUser(userData);
        return { success: true, user: userData };
      }
      return { success: false, error: "Respuesta inválida del servidor" };
    } catch (err) {
      const message = err.data?.message || err.message || "Error al iniciar sesión";
      // `code` distingue el 401 de cuenta desactivada de cualquier otro 401 (credenciales
      // malas, usuario inexistente) — LoginPage decide el mensaje mostrado según esto, nunca lo
      // deduce por su cuenta (NOTAS-PARA-FRONTEND.md §3, docs/PLAN-IMPLEMENTACION.md §5.1).
      return { success: false, error: message, code: err.code };
    }
  }

  function logout() {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
    setUser(null);
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
    cancelProactiveRefresh();
    // Revoca el refresh token en el servidor — sin esto, quien lo hubiera copiado antes de
    // cerrar sesión seguiría pudiendo renovarla. Best-effort: no bloquea el logout local si
    // falla o si el endpoint todavía no existe (misma bandera que el resto de la Fase 4).
    if (REFRESH_ENABLED && refreshToken) {
      authService.logout(refreshToken).catch((err) => {
        console.warn("No se pudo revocar la sesión en el servidor:", err);
      });
    }
  }

  function hasRole(roleName) {
    return user ? user.roles.includes(roleName) : false;
  }

  function updateUserProfile(profileUpdates) {
    if (!user) return;
    setUser((prev) => ({
      ...prev,
      ...profileUpdates,
    }));
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        logout,
        hasRole,
        updateUserProfile,
        isAuthenticated: !!user,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth debe ser usado dentro de un AuthProvider");
  }
  return context;
}
