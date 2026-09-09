// admin_sessions.jsx (hoy SessionsPage) — profesores mock. Se reemplaza
// por GET /api/usuarios/profesores cuando se conecte el service real.
export const TEACHERS = [
  { userId: 12, fullName: "Laura Gómez Restrepo" },
  { userId: 18, fullName: "Camilo Pérez Londoño" },
];

export function teacherName(userId) {
  return TEACHERS.find((t) => t.userId === userId)?.fullName ?? "Sin asignar";
}

// admin_schedule.jsx (hoy SchedulePage)
export function timeLabel(start, end) {
  return `${start} – ${end}`;
}
