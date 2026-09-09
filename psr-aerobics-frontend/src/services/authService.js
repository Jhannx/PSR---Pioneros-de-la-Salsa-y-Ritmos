import { apiClient } from "./apiClient";

export const authService = {
  login: async (identifier, password) => {
    return await apiClient.post("/api/auth/login", { identifier, password });
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
