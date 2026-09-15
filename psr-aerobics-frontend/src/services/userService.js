import { apiClient } from "./apiClient";

export const userService = {
  getTeachers: async () => {
    return await apiClient.get("/api/usuarios/profesores");
  },

  getRoles: async () => {
    return await apiClient.get("/api/roles");
  },

  getUserByPersonId: async (personId) => {
    return await apiClient.get(`/api/usuarios/persona/${personId}`);
  },

  getById: async (userId) => {
    return await apiClient.get(`/api/usuarios/${userId}`);
  },

  getUserRoles: async (userId) => {
    return await apiClient.get(`/api/usuarios/${userId}/roles`);
  },

  addRole: async (userId, roleName) => {
    return await apiClient.post(`/api/usuarios/${userId}/roles`, { roleName });
  },

  removeRole: async (userId, roleName) => {
    return await apiClient.delete(`/api/usuarios/${userId}/roles/${roleName}`);
  },

  activateUser: async (userId) => {
    return await apiClient.patch(`/api/usuarios/${userId}/activar`);
  },

  deactivateUser: async (userId) => {
    return await apiClient.patch(`/api/usuarios/${userId}/desactivar`);
  },
};
