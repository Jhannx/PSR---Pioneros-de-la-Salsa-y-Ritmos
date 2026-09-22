import { toLocalDateString } from "../../utils/date";

// Utilidades de PRESENTACIÓN para mostrar sesiones por semana (docs/correciones 20-09.md): armar
// los siete días de lunes a domingo, ponerles nombre y agrupar las sesiones por día. No deciden nada
// del negocio — qué sesiones existen, cuáles están activas o cuál puede finalizarse lo dice el
// servidor (`status`, `estado`).

// La semana (lunes a domingo, en hora local) que queda `offsetWeeks` semanas de la actual: 0 es esta
// semana, -1 la anterior, 1 la siguiente. Devuelve el rango `desde`/`hasta` para pedirle al
// servidor y los siete días como "YYYY-MM-DD".
export function getWeekRange(offsetWeeks = 0, base = new Date()) {
  const monday = new Date(base.getFullYear(), base.getMonth(), base.getDate());
  const daysSinceMonday = (monday.getDay() + 6) % 7; // lunes = 0 … domingo = 6
  monday.setDate(monday.getDate() - daysSinceMonday + offsetWeeks * 7);

  const days = Array.from({ length: 7 }, (_, index) => {
    const day = new Date(monday);
    day.setDate(monday.getDate() + index);
    return toLocalDateString(day);
  });

  return { desde: days[0], hasta: days[6], days };
}

function capitalize(text) {
  return text ? text.charAt(0).toUpperCase() + text.slice(1) : text;
}

// "2026-09-14" → "Lunes 14 sep"
export function formatDayHeading(isoDate) {
  const date = new Date(`${isoDate}T00:00:00`);
  const weekday = date.toLocaleDateString("es-CO", { weekday: "long" });
  const month = date.toLocaleDateString("es-CO", { month: "short" }).replace(".", "");
  return `${capitalize(weekday)} ${date.getDate()} ${month}`;
}

// "2026-09-14" → { weekdayShort: "LUN", weekday: "Lunes", day: 14, month: "sep" } — para pintar el
// bloque de fecha de cada día.
export function formatDayParts(isoDate) {
  const date = new Date(`${isoDate}T00:00:00`);
  const weekday = date.toLocaleDateString("es-CO", { weekday: "long" });
  return {
    weekdayShort: weekday.slice(0, 3).toUpperCase(),
    weekday: capitalize(weekday),
    day: date.getDate(),
    month: date.toLocaleDateString("es-CO", { month: "short" }).replace(".", ""),
  };
}

// ("2026-09-14", "2026-09-20") → "14 sep – 20 sep 2026"
export function formatRangeLabel(desde, hasta) {
  const from = new Date(`${desde}T00:00:00`);
  const to = new Date(`${hasta}T00:00:00`);
  const short = (date) =>
    `${date.getDate()} ${date.toLocaleDateString("es-CO", { month: "short" }).replace(".", "")}`;
  return `${short(from)} – ${short(to)} ${to.getFullYear()}`;
}

// Agrupa por fecha ("YYYY-MM-DD") y ordena cada día por hora de inicio.
export function groupSessionsByDate(sessions) {
  const groups = new Map();
  for (const session of sessions) {
    if (!groups.has(session.date)) groups.set(session.date, []);
    groups.get(session.date).push(session);
  }
  for (const group of groups.values()) {
    group.sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
  }
  return groups;
}
