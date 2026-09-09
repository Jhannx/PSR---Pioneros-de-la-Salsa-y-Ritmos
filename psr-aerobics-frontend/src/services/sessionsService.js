import { apiClient } from "./apiClient";

export const sessionsService = {
  getSessions: async (desde, hasta) => {
    const params = new URLSearchParams();
    if (desde) params.append("desde", desde);
    if (hasta) params.append("hasta", hasta);
    const queryString = params.toString();
    return await apiClient.get(`/api/sesiones${queryString ? `?${queryString}` : ""}`);
  },

  getSessionSummary: async (id) => {
    return await apiClient.get(`/api/sesiones/${id}/resumen`);
  },

  createSession: async (sessionData) => {
    // SessionRequest: { scheduleId, date, startTime, endTime, teacherUserId }
    return await apiClient.post("/api/sesiones", sessionData);
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
};
