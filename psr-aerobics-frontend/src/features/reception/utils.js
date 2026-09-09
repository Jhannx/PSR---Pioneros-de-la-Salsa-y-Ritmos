import { capitalize } from "../../utils/text";

// Tarifa de respaldo si GET /api/tarifa no responde a tiempo — ReceptionCheckinPage
// siempre intenta cargar la tarifa real primero y solo usa esto como valor inicial.
export const DAILY_RATE = 4000;

export function getUpcomingDays(count) {
  const weekdayFormatter = new Intl.DateTimeFormat("es-CO", { weekday: "short" });
  const today = new Date();
  const days = [];
  for (let i = 0; i < count; i++) {
    const d = new Date(today);
    d.setDate(today.getDate() + i);
    days.push({
      weekday: capitalize(weekdayFormatter.format(d).replace(".", "")),
      date: d.getDate(),
    });
  }
  return days;
}
