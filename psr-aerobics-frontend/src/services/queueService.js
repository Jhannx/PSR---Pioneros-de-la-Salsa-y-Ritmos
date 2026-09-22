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

  // Por defecto el servidor devuelve SOLO la sesión vigente — la que está en curso, si no la
  // próxima, si no la última del día; cuál es lo decide él (NOTAS-PARA-FRONTEND.md Bloque 4,
  // punto 5.3). `sesionId` pide una concreta y `todas: true` el día entero (lo que devolvía este
  // endpoint antes de ese cambio).
  getAdminQueue: async ({ sesionId, todas } = {}) => {
    const params = new URLSearchParams();
    if (sesionId) params.append("sesionId", sesionId);
    if (todas) params.append("todas", "true");
    const query = params.toString();
    return await apiClient.get(`/api/cola/admin${query ? `?${query}` : ""}`);
  },

  // Resumen de la cola (NOTAS-PARA-FRONTEND.md Bloque 6, puntos 3 y 4). STAFF:
  //   { session: { id, startTime, endTime, status, teacherUserId, teacherName, totalCheckins,
  //                totalAttended, canClose, canCancel } | null,
  //     collectedThisSession, collectedToday }
  // `session` es la VIGENTE según el servidor (en curso; si no, la próxima de hoy; si no, la última de
  // hoy) y sale de la tabla de sesiones, no de las filas: existe aunque nadie haya llegado. Las dos
  // cifras vienen en `null` para un PROFESOR (no tiene reportes de ingresos). `canClose`/`canCancel`
  // vienen en `null` en esta respuesta (sí se calculan en `/api/sesiones`).
  getSummary: async () => {
    return await apiClient.get("/api/cola/resumen");
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
    // (NOTAS-PARA-FRONTEND.md §1).
    //
    // Es seguro de repetir (NOTAS-PARA-FRONTEND.md Bloque 4, punto 1): 201 si la llegada se registró
    // ahora, 200 si ya la tenía (devuelve la misma, no crea otra). Devuelve `OwnCheckinResponse`:
    // { checkinId, sessionId, arrivalTime, queuePosition, attended, amountCharged, sessionStatus }.
    // Fuera de horario responde 400 con "No hay ninguna clase programada en este momento".
    return await apiClient.post("/api/checkin/propio");
  },

  // La llegada de hoy de quien está logueado, con la misma forma que el POST de arriba — es lo que
  // hace que la pantalla del miembro sobreviva a un F5. 204 si hoy no ha marcado llegada: `request`
  // devuelve `null` en ese caso (no es un error, no hay que mostrar ningún mensaje de fallo).
  getOwnCheckin: async () => {
    return await apiClient.get("/api/checkin/propio");
  },

  // "Agregar manual" de recepción, por número de documento. Registra con método MANUAL (no
  // DOCUMENT) y es el único canal que puede registrar fuera de horario: entra a la última sesión
  // del día en vez de rechazar (NOTAS-PARA-FRONTEND.md Bloque 4, puntos 2 y 5.1). DESK.
  checkinManualByDocument: async (documentNumber) => {
    return await apiClient.post("/api/checkin/manual/documento", { documentNumber });
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
