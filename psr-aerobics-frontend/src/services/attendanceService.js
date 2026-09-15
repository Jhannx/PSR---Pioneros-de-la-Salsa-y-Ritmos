import { apiClient } from "./apiClient";

export const attendanceService = {
  getByCheckinId: async (checkinId) => {
    return await apiClient.get(`/api/llegadas/${checkinId}/asistencia`);
  },

  confirm: async (checkinId) => {
    // AttendanceResponse: { attendanceId, checkinId, personId, fullName, amountCharged, markedByUserId, markedByName, markedAt }
    return await apiClient.put(`/api/llegadas/${checkinId}/asistencia`);
  },

  cancel: async (checkinId) => {
    return await apiClient.delete(`/api/llegadas/${checkinId}/asistencia`);
  },
};
