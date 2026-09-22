import { apiClient } from "./apiClient";

export const paymentsService = {
  // TODOS los métodos, activos e inactivos — es lo que necesita la pestaña de administración para
  // poder mostrar el estado de cada uno y reactivarlos. NO usar para llenar un selector: un método
  // desactivado seguiría apareciendo (así pasó el 19-09 en el reembolso y en el cobro).
  getAllPaymentMethods: async () => {
    return await apiClient.get("/api/metodos-pago");
  },

  // Solo los ACTIVOS — para cualquier selector o cobro. Es el mismo endpoint con un parámetro
  // (`?activo=true`), la ruta exacta que confirmó el backend (NOTAS-PARA-FRONTEND.md Bloque 3,
  // 2.1): el filtro vive en el servidor, no en el cliente. Antes se filtraba acá a mano mientras
  // ese parámetro no se sabía que existía. Devuelve la misma forma de datos que el listado general.
  getActivePaymentMethods: async () => {
    const active = await apiClient.get("/api/metodos-pago?activo=true");
    return Array.isArray(active) ? active : [];
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
    // No recibe cuerpo — entrega el vuelto pendiente del movimiento original. Salda el vuelto
    // ENTERO de ese movimiento: no hay entrega parcial en v1 (NOTAS-PARA-FRONTEND.md Bloque 4, 3.5).
    return await apiClient.post(`/api/movimientos/${movementId}/vuelto`);
  },

  // Convierte vuelto pendiente en abono (NOTAS-PARA-FRONTEND.md Bloque 4, 3.4): "abonar N días con
  // el mismo dinero". `movementId` es el movimiento del COBRO, no el de la persona. Acepta parcial
  // (el resto sigue pendiente). No lleva método de pago a propósito: aquí no entra dinero, se sube
  // el monto abonado del mismo movimiento, así que el recaudo no se mueve. 400 si es un reembolso,
  // si el vuelto ya se entregó, si no hay vuelto pendiente o si piden más de lo que queda. DESK.
  payChangeAsCredit: async (movementId, data) => {
    // { amount, note? }
    return await apiClient.post(`/api/movimientos/${movementId}/abonar-vuelto`, data);
  },

  // Reembolsa contra UN movimiento puntual — sigue existiendo para cuando de verdad se quiera
  // apuntar a un abono concreto. Para "devolverle X a esta persona" en general, usar
  // `refundPerson` de abajo (RESPUESTA-NOTAS-FINAL.md #1).
  processRefund: async (movementId, data) => {
    // RefundRequest: { paymentMethodId, amount, note }
    return await apiClient.post(`/api/movimientos/${movementId}/reembolso`, data);
  },

  // Reembolsa un monto total sin importar en cuántos abonos esté repartido — el backend decide el
  // reparto (FIFO, todo o nada) en una sola transacción. Reemplaza el parche que hacía esto mismo
  // desde el front, encadenando una llamada por movimiento (RESPUESTA-NOTAS-FINAL.md #1).
  refundPerson: async (personId, data) => {
    // { amount, paymentMethodId, note? } → { personId, totalRefunded, allocations: [...] }
    return await apiClient.post(`/api/personas/${personId}/reembolso`, data);
  },

  getPricing: async () => {
    return await apiClient.get("/api/tarifa");
  },

  updatePricing: async (pricingData) => {
    return await apiClient.put("/api/tarifa", pricingData);
  },
};
