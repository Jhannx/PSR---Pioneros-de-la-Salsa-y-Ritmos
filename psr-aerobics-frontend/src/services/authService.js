import { apiClient } from "./apiClient";

export const authService = {
  login: async (identifier, password) => {
    // Con sesión persistente (docs/notas-para-back.md §1), la respuesta trae además
    // `refreshToken` y `expiresIn`. Mientras el backend no los mande, quedan `undefined` y
    // AuthContext simplemente no los guarda — no rompe nada.
    return await apiClient.post("/api/auth/login", { identifier, password });
  },

  // Revoca el refresh token del lado del servidor. Sin esto, "cerrar sesión" solo borra el
  // token del navegador: quien lo hubiera copiado antes seguiría pudiendo renovar la sesión.
  // apiClient.request ya maneja la renovación automática de 401 por su cuenta (ver
  // services/apiClient.js); esta llamada es aparte y solo se usa al cerrar sesión a propósito.
  logout: async (refreshToken) => {
    return await apiClient.post("/api/auth/logout", { refreshToken });
  },

  forgotPassword: async (email) => {
    return await apiClient.post("/api/auth/forgot-password", { email });
  },

  resetPassword: async (token, newPassword) => {
    return await apiClient.post("/api/auth/reset-password", { token, newPassword });
  },

  changePassword: async (currentPassword, newPassword) => {
    return await apiClient.patch("/api/auth/change-password", { currentPassword, newPassword });
  },
};
