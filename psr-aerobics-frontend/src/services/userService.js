import { apiClient } from "./apiClient";

export const userService = {
  getTeachers: async () => {
    return await apiClient.get("/api/usuarios/profesores");
  },

  // Desde NOTAS-PARA-FRONTEND.md (ronda 5, 13-sep): GET /api/usuarios ya trae `roles` (array de
  // strings en inglés, sin prefijo ROLE_) y `active` en la misma respuesta paginada — una sola
  // consulta para toda la página, no una por usuario. Reemplaza el patrón N+1 que tenía
  // `RolesPage.jsx` (`getUserRoles` + `getById` por cada fila).
  getAll: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    return await apiClient.get(`/api/usuarios${query ? `?${query}` : ""}`);
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
