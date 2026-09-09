import { apiClient } from "./apiClient";

export const queueService = {
  getPendientes: async (sesionId) => {
    return await apiClient.get(`/api/cola/pendientes${sesionId ? `?sesionId=${sesionId}` : ""}`);
  },

  getWaiting: async () => {
    return await apiClient.get("/api/cola/espera");
  },

  getPrepaid: async () => {
    return await apiClient.get("/api/cola/prepagada");
  },

  getAdminQueue: async () => {
    return await apiClient.get("/api/cola/admin");
  },

  checkin: async (personId) => {
    // CheckinRequest: { personId } — registro por Terminal PSR (documento propio)
    return await apiClient.post("/api/cola", { personId });
  },

  checkinManual: async (personId) => {
    // CheckinRequest: { personId } — registro manual desde recepción
    return await apiClient.post("/api/checkin/manual", { personId });
  },

  checkinByDocument: async (documentNumber) => {
    // CheckinByDocumentRequest: { documentNumber }
    return await apiClient.post("/api/checkin/documento", { documentNumber });
  },

  getConfig: async () => {
    return await apiClient.get("/api/cola/configuracion");
  },

  updateConfig: async (config) => {
    return await apiClient.put("/api/cola/configuracion", config);
  },
};
