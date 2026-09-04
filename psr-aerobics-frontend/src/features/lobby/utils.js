// vista_cola_sala_espera.jsx (hoy LobbyQueueDisplayPage) — helper de
// datos mock, se elimina cuando se conecte el service real.
export function minutesAgo(n) {
  return new Date(Date.now() - n * 60 * 1000);
}

export function turnMessage(count) {
  if (count <= 1) return "Pasa 1 en 1 — siempre el primero de la fila";
  return `Pasan de ${count} en ${count} — siempre los primeros de la fila`;
}
