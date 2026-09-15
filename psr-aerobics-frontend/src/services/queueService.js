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

  checkinPropio: async () => {
    // Sin cuerpo — la persona sale del token, no hay nada que mandar ni que falsificar.
    // Autorizado solo para MEMBER. `POST /api/cola` (arriba) recibe `personId` en el cuerpo y
    // por eso sigue cerrado a ese rol a propósito: si se le diera acceso, cualquier miembro
    // podría marcarle la llegada a otra persona cambiando ese número
    // (NOTAS-PARA-FRONTEND.md §1). Responde 201 con el mismo CheckinResponse, queuePosition real.
    return await apiClient.post("/api/checkin/propio");
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
