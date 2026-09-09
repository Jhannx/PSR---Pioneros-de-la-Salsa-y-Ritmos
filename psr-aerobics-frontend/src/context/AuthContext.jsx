/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect } from "react";
import { authService } from "../services/authService";

const AUTH_STORAGE_KEY = "psr_aerobics_user_session_v2";
const TOKEN_STORAGE_KEY = "psr_auth_token";

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

  async function login(identifier, password) {
    try {
      const response = await authService.login(identifier, password);
      if (response && response.token && response.user) {
        localStorage.setItem(TOKEN_STORAGE_KEY, response.token);
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
      return { success: false, error: message };
    }
  }

  function logout() {
    setUser(null);
    localStorage.removeItem(TOKEN_STORAGE_KEY);
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
