import { apiClient } from "./apiClient";

export const paymentsService = {
  getPaymentMethods: async () => {
    return await apiClient.get("/api/metodos-pago");
  },

  createPaymentMethod: async (data) => {
    return await apiClient.post("/api/metodos-pago", data);
  },

  updatePaymentMethod: async (id, data) => {
    return await apiClient.put(`/api/metodos-pago/${id}`, data);
  },

  togglePaymentMethod: async (id, active) => {
    return await apiClient.patch(`/api/metodos-pago/${id}/${active ? "activar" : "desactivar"}`);
  },

  createMovement: async (data) => {
    // AccountMovementRequest: { personId, checkinId?, paymentMethodId, amountReceived, amountApplied, note }
    return await apiClient.post("/api/movimientos", data);
  },

  getMovementsByCheckin: async (checkinId) => {
    return await apiClient.get(`/api/movimientos/checkin/${checkinId}`);
  },

  getPendingChanges: async () => {
    return await apiClient.get("/api/movimientos/pendientes/vueltos");
  },

  deliverChange: async (movementId) => {
    // No recibe cuerpo — entrega el vuelto pendiente del movimiento original.
    return await apiClient.post(`/api/movimientos/${movementId}/vuelto`);
  },

  processRefund: async (movementId, data) => {
    // RefundRequest: { paymentMethodId, amount, note }
    return await apiClient.post(`/api/movimientos/${movementId}/reembolso`, data);
  },

  getPricing: async () => {
    return await apiClient.get("/api/tarifa");
  },

  updatePricing: async (pricingData) => {
    return await apiClient.put("/api/tarifa", pricingData);
  },
};
