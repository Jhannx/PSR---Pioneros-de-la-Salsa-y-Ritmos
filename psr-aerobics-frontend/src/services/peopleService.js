import { apiClient } from "./apiClient";

export const peopleService = {
  getAll: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    return await apiClient.get(`/api/personas${query ? `?${query}` : ""}`);
  },

  getById: async (id) => {
    return await apiClient.get(`/api/personas/${id}`);
  },

  getProfile: async (id) => {
    return await apiClient.get(`/api/personas/${id}/perfil`);
  },

  getBalance: async (id) => {
    return await apiClient.get(`/api/personas/${id}/saldo`);
  },

  create: async (data) => {
    // PersonRequest: { documentType, documentNumber, firstName, lastName, phone }
    return await apiClient.post("/api/personas", data);
  },

  autoRegister: async (data) => {
    // PersonRequest: { documentType, documentNumber, firstName, lastName, phone }
    return await apiClient.post("/api/personas/autoregistro", data);
  },

  update: async (id, data) => {
    // PersonUpdateRequest: { documentType, documentNumber, firstName, lastName, phone }
    return await apiClient.put(`/api/personas/${id}`, data);
  },

  updateEmergencyContact: async (id, data) => {
    // EmergencyContactRequest: { name, phone, observations }
    return await apiClient.put(`/api/personas/${id}/contacto-emergencia`, data);
  },

  getEmergencyContact: async (id) => apiClient.get(`/api/personas/${id}/contacto-emergencia`),

  deactivate: async (id) => {
    return await apiClient.patch(`/api/personas/${id}/desactivar`);
  },

  reactivate: async (id) => {
    return await apiClient.patch(`/api/personas/${id}/reactivar`);
  },

  remove: async (id) => apiClient.delete(`/api/personas/${id}`),
};
