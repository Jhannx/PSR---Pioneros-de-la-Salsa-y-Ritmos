import { capitalize } from "../../utils/text";

// admin_checkin_desktop.jsx (hoy ReceptionCheckinPage) — tarifa mock.
// Cuando se conecte el service real, esto se reemplaza por lo que
// devuelva GET /api/tarifa, no se sigue hardcodeando acá.
export const DAILY_RATE = 4000;

// El saldo depende SOLO del monto registrado — nunca de si ya fue
// atendido. Se puede deber dinero estando atendido, y se puede estar al
// día sin haber sido atendido todavía (pagó antes de que le tocara).
export function getBalance(person) {
  return Number(person.amount || 0) - DAILY_RATE;
}

export function getSourceLabel(source) {
  if (source === "manual") return "manual · sin dispositivo";
  return "QR";
}

export function getRowStyle(balance) {
  if (balance === 0) return "bg-emerald-500/10 border-emerald-500/30";
  if (balance < 0) return "bg-red-500/10 border-red-500/30";
  return "bg-sky-500/10 border-sky-500/30";
}

export function matchesFilter(person, filterKey) {
  if (filterKey === "pending") return !person.atendido;
  if (filterKey === "balance") return getBalance(person) !== 0;
  if (filterKey === "settled") return getBalance(person) === 0;
  return true; // "all"
}

export function computeCounts(queue) {
  return {
    all: queue.length,
    pending: queue.filter((p) => !p.atendido).length,
    balance: queue.filter((p) => getBalance(p) !== 0).length,
    settled: queue.filter((p) => getBalance(p) === 0).length,
  };
}

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

// admin_people_registry.jsx (hoy PeopleListPage) — helpers de datos mock,
// se eliminan cuando se conecte el service real.
export function daysAgo(n) {
  return new Date(Date.now() - n * 24 * 60 * 60 * 1000).toISOString();
}

export function randomPhone() {
  return `300${Math.floor(1000000 + Math.random() * 8999999)}`;
}

export function randomDocumento() {
  return `10${Math.floor(10000000 + Math.random() * 89999999)}`;
}
