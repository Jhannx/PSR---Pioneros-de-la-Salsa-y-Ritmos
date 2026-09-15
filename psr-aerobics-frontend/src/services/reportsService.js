import { apiClient } from "./apiClient";

export const reportsService = {
  getDashboard: async () => {
    return await apiClient.get("/api/reportes/dashboard");
  },

  getAuditHistory: async (params = {}) => {
    const query = new URLSearchParams(params).toString();
    return await apiClient.get(`/api/auditoria/historial-cambios${query ? `?${query}` : ""}`);
  },

  getRevenueBySession: async (desde, hasta) => {
    const params = new URLSearchParams();
    if (desde) params.append("desde", desde);
    if (hasta) params.append("hasta", hasta);
    const queryString = params.toString();
    return await apiClient.get(`/api/reportes/recaudo-por-sesion${queryString ? `?${queryString}` : ""}`);
  },

  getDailyRevenue: async (desde, hasta) => {
    const params = new URLSearchParams();
    if (desde) params.append("desde", desde);
    if (hasta) params.append("hasta", hasta);
    const queryString = params.toString();
    return await apiClient.get(`/api/reportes/recaudo-diario${queryString ? `?${queryString}` : ""}`);
  },

  getPersonBalances: async ({ page = 0, size = 20 } = {}) => {
    // PagePersonBalanceViewResponse: { content: [{personId, firstName, lastName, totalPaid, totalCharged, balance}], ... }
    return await apiClient.get(`/api/reportes/saldos-personas?page=${page}&size=${size}`);
  },
};
