import { apiClient } from "./apiClient";

export const sessionsService = {
  // GET /api/sesiones SIN parámetros es un histórico completo, ordenado por fecha DESCENDENTE y
  // paginado de 20 en 20 (RESPUESTA-NOTAS-FINAL.md #2) — desde que las sesiones se generan 7 días
  // por adelantado, la página 0 se llena de sesiones futuras y la de HOY cae a la página 2, dando
  // la impresión de que se "borró" sin que nada se haya borrado. `desde`/`hasta` van siempre juntos
  // (o ninguno) — `size` grande de respaldo por si el rango igual pagina.
  //
  // `estado` (NOTAS-PARA-FRONTEND.md Bloque 4, punto 6): "activas" (lo que el servidor asume si no se
  // manda), "canceladas" o "todas"; cualquier otro valor da 400. Cada sesión trae su `status`
  // (UPCOMING / IN_PROGRESS / ENDED / CANCELLED), calculado con el reloj del servidor.
  getSessions: async (desde, hasta, size = 200, estado) => {
    const params = new URLSearchParams();
    if (desde) params.append("desde", desde);
    if (hasta) params.append("hasta", hasta);
    if (size) params.append("size", size);
    if (estado) params.append("estado", estado);
    const queryString = params.toString();
    return await apiClient.get(`/api/sesiones${queryString ? `?${queryString}` : ""}`);
  },

  // Solo las sesiones del profesor logueado — sale del TOKEN, no lleva su id en la ruta a propósito
  // (así ningún profesor puede ver la agenda de otro cambiando un número). Paginado (`content`),
  // con el mismo `status`. No trae totales: esos vienen de `getSessionSummaries`.
  getMySessions: async (desde, hasta, size = 200) => {
    const params = new URLSearchParams();
    if (desde) params.append("desde", desde);
    if (hasta) params.append("hasta", hasta);
    if (size) params.append("size", size);
    return await apiClient.get(`/api/sesiones/mias?${params.toString()}`);
  },

  // Totales por sesión (`totalCheckins` / `totalAttended`) para un rango de fechas.
  getSessionSummaries: async (desde, hasta) => {
    const params = new URLSearchParams();
    if (desde) params.append("desde", desde);
    if (hasta) params.append("hasta", hasta);
    return await apiClient.get(`/api/sesiones/resumenes?${params.toString()}`);
  },

  // Quién llegó a una sesión, incluida la gente que llegó y NO fue confirmada (`attended: false`):
  // [{ personId, fullName, arrivalTime, attended, markedAt, amountCharged }]. STAFF.
  getSessionAttendees: async (id) => {
    return await apiClient.get(`/api/sesiones/${id}/asistentes`);
  },

  // Sesiones de un solo día — no pagina, no puede esconder ninguna (RESPUESTA-NOTAS-FINAL.md #2).
  getSessionsByDate: async (fecha) => {
    return await apiClient.get(`/api/sesiones/fecha/${fecha}`);
  },

  getSessionSummary: async (id) => {
    return await apiClient.get(`/api/sesiones/${id}/resumen`);
  },

  createSession: async (sessionData) => {
    // SessionRequest: { date: LocalDate "YYYY-MM-DD", startTime: OffsetDateTime (obligatorio),
    // endTime: OffsetDateTime (opcional, sesión queda en curso si falta), teacherUserId: int
    // (opcional, debe tener rol TEACHER), scheduleId: int (opcional; si viene, máximo una
    // sesión por horario y fecha) — NOTAS-PARA-FRONTEND.md §1. `date` y `startTime` llevan la
    // fecha repetida a propósito (confirmado por backend, redundante pero así es el contrato).
    // Construir startTime/endTime con utils/date.js#toOffsetDateTime, no a mano.
    return await apiClient.post("/api/sesiones", sessionData);
  },

  // Dos operaciones distintas, no intercambiables (NOTAS-PARA-FRONTEND.md §6.6): cancelar marca
  // la sesión cancelada sin tocar horas y vale siempre, incluso antes de que empiece; cerrar le
  // pone hora_fin = ahora y solo tiene sentido si ya empezó. El botón "Cancelar" llamaba por
  // error a closeSession, lo que reventaba contra chk_sesion_horas en sesiones que aún no
  // empezaban.
  cancelSession: async (id) => {
    return await apiClient.delete(`/api/sesiones/${id}`);
  },

  closeSession: async (id) => {
    return await apiClient.put(`/api/sesiones/${id}/cerrar`);
  },

  reassignTeacher: async (id, teacherUserId) => {
    return await apiClient.put(`/api/sesiones/${id}/profesor/${teacherUserId}`);
  },

  getSchedules: async () => {
    return await apiClient.get("/api/horarios");
  },

  createSchedule: async (scheduleData) => {
    // ScheduleRequest: { dayOfWeek, startTime, endTime }
    return await apiClient.post("/api/horarios", scheduleData);
  },

  updateSchedule: async (id, scheduleData) => {
    // ScheduleRequest: { dayOfWeek, startTime, endTime }
    return await apiClient.put(`/api/horarios/${id}`, scheduleData);
  },

  deleteSchedule: async (id) => {
    return await apiClient.delete(`/api/horarios/${id}`);
  },

  getScheduleExceptions: async (scheduleId) => {
    return await apiClient.get(`/api/horarios/${scheduleId}/excepciones`);
  },

  createScheduleException: async (scheduleId, data) => {
    // ScheduleExceptionRequest: { date, reason }
    return await apiClient.post(`/api/horarios/${scheduleId}/excepciones`, data);
  },

  deleteScheduleException: async (exceptionId) => {
    return await apiClient.delete(`/api/horarios/excepciones/${exceptionId}`);
  },

  // Barrido manual (NOTAS-PARA-FRONTEND.md Bloque 2, A3) — desde este round, crear/editar/activar
  // un horario ya dispara la generación de sus sesiones solo, así que esto es para el caso raro
  // en que haga falta forzarlo (soporte, o el barrido automático horario todavía no pasó).
  // Idempotente del lado del backend: lo que ya existe no se duplica. Responde
  // { message: "N sesión(es) creada(s)." }. ADMIN únicamente.
  generateSessions: async () => {
    return await apiClient.post("/api/horarios/generar-sesiones");
  },
};
