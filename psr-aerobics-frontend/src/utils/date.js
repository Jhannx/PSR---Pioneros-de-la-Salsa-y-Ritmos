// Cada función de acá viene de un mock distinto. Se mantienen separadas
// aunque compartan nombre de origen, porque el formato exacto no es
// igual entre unas y otras — fusionarlas habría cambiado lo que ya se
// veía en pantalla.

// admin_checkin_desktop.jsx → formatHeaderDate (dd/mm/aa)
export function formatHeaderDateShort(date) {
  const dd = String(date.getDate()).padStart(2, "0");
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const yy = String(date.getFullYear()).slice(-2);
  return `${dd}/${mm}/${yy}`;
}

// vista_checkin_terminal.jsx → formatTime (con segundos)
export function formatTimeWithSeconds(date) {
  return date.toLocaleTimeString("es-CO", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

// vista_cola_sala_espera.jsx → formatTime (sin segundos)
export function formatTimeShort(date) {
  return date.toLocaleTimeString("es-CO", { hour: "2-digit", minute: "2-digit" });
}

// vista_cola_sala_espera.jsx → formatHeaderDate (cabecera de la pantalla TV)
export function formatHeaderDateLong(date) {
  const weekday = date.toLocaleDateString("es-CO", { weekday: "long" });
  const day = date.getDate();
  const month = date.toLocaleDateString("es-CO", { month: "long" });
  return `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)} ${day} de ${month}`;
}

// vista_cola_sala_espera.jsx → formatHeaderTime (reloj en vivo, 24h)
export function formatHeaderTime24h(date) {
  return date.toLocaleTimeString("es-CO", { hour: "2-digit", minute: "2-digit", hour12: false });
}

// admin_sessions.jsx y admin_reports.jsx → formatDate (idénticas en ambas, se fusionan en una sola)
export function formatShortWeekdayDate(dateStr) {
  const date = new Date(`${dateStr}T00:00:00`);
  return date.toLocaleDateString("es-CO", { weekday: "short", day: "2-digit", month: "short" });
}

// admin_audit.jsx → formatDateTime
export function formatDateTimeShort(dateStr) {
  const date = new Date(dateStr);
  return date.toLocaleString("es-CO", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
}

// Fecha de nacimiento (docs/PLAN-IMPLEMENTACION.md §5.3) — campo nuevo, sin mock de origen.
// Puede llegar `null`: las personas registradas antes de que el campo existiera no lo tienen.
export function formatBirthDate(dateStr) {
  if (!dateStr) return null;
  const date = new Date(`${dateStr}T00:00:00`);
  return date.toLocaleDateString("es-CO", { day: "2-digit", month: "long", year: "numeric" });
}

// `date` ("YYYY-MM-DD") + `time` ("HH:mm") → OffsetDateTime ISO-8601 con el offset de zona
// horaria del navegador (ej. "2026-09-10T06:00:00-05:00"). NOTAS-PARA-FRONTEND.md §1: el error
// "cuerpo de la solicitud JSON inválido" al crear sesiones no era de contenido, era de formato
// — Jackson espera OffsetDateTime y el frontend mandaba fecha+hora sin offset
// ("2026-09-10T06:00:00"), que ni siquiera llega al controller. No se hardcodea "-05:00": se
// calcula del reloj del navegador con `getTimezoneOffset()` (que da minutos *detrás* de UTC, de
// ahí el signo invertido), para que siga siendo correcto si la app corre en otra zona horaria.
export function toOffsetDateTime(dateStr, timeStr) {
  const local = new Date(`${dateStr}T${timeStr}:00`);
  const offsetMinutes = -local.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? "+" : "-";
  const abs = Math.abs(offsetMinutes);
  const hh = String(Math.floor(abs / 60)).padStart(2, "0");
  const mm = String(abs % 60).padStart(2, "0");
  return `${dateStr}T${timeStr}:00${sign}${hh}:${mm}`;
}

// admin_sessions.jsx mostraba `session.startTime`/`endTime` crudos (OffsetDateTime ISO en UTC,
// ej. "2026-09-11T20:45:00Z") en vez de la hora local que la persona digitó — mala UX señalada en
// errores-nuevo-compartido.md #1. `Date` ya interpreta el offset/`Z` del string al convertir a
// hora local, así que basta con formatear con `toLocaleTimeString`.
export function formatTimeOfDay(isoDateTimeStr) {
  return new Date(isoDateTimeStr).toLocaleTimeString("es-CO", { hour: "numeric", minute: "2-digit" });
}

// Fecha límite exacta para exigir mínimo `years` años de edad frente a HOY (no una diferencia de
// año a secas: alguien nacido hace 9 años y 11 meses no cumple 10, aunque el año ya diste 10)
// (errores-nuevo-front.md, nota suelta "Cambiar Selector de fechas"). Comparación lexicográfica
// de strings "YYYY-MM-DD" funciona igual que comparar fechas reales.
/** "YYYY-MM-DD" en hora local — para `?desde=`/`?hasta=` de GET /api/sesiones (LocalDate). */
export function toLocalDateString(date) {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

export function getMaxBirthDateForAge(years) {
  const today = new Date();
  const cutoff = new Date(today.getFullYear() - years, today.getMonth(), today.getDate());
  const yyyy = cutoff.getFullYear();
  const mm = String(cutoff.getMonth() + 1).padStart(2, "0");
  const dd = String(cutoff.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}
