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
