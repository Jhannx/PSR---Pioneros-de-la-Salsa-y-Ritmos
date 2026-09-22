import { capitalize } from "../../utils/text";

// Tarifa de respaldo si GET /api/tarifa no responde a tiempo — ReceptionCheckinPage
// siempre intenta cargar la tarifa real primero y solo usa esto como valor inicial.
export const DAILY_RATE = 4000;

// Tope de lo que se le puede devolver a una persona desde recepción: el `refundable` que manda el
// backend, tal cual. Ya viene acotado — el servidor lo calcula como el menor entre lo abonado que
// sigue sin devolverse y el saldo a favor, y 0 si el saldo es cero o negativo, con la misma
// función que usa para validar el reembolso (NOTAS-PARA-FRONTEND.md Bloque 3, punto 1), así que
// pedir más da 400 desde el servidor y no solo se recorta en pantalla.
//
// Historia, por si vuelve a hacer falta: durante un día este helper tomaba el menor entre
// `balance` y `refundable`, porque el `refundable` de entonces no descontaba lo ya consumido en
// asistencias y dejaba devolver $15.000 a alguien con saldo de $5.000 (la cuenta quedaba
// debiendo). Se corrigió en el backend y ese recorte se quitó a propósito.
//
// Sin `refundable` en la respuesta el tope es 0 (botón apagado) en vez de adivinar con `balance`:
// tratándose de dinero, que un contrato roto se note es mejor que devolver de más.
export function getRefundCap(item) {
  return Math.max(0, Number(item?.refundable) || 0);
}

// Cuánto hay que cobrarle a la persona AHORA (`amountDue` en cada fila de la cola, NOTAS-PARA-FRONTEND.md
// Bloque 5, punto 1): su deuda anterior y su saldo a favor ya están descontados, 0 si no hay nada que
// cobrar. Se usa TAL CUAL: la regla vive en el servidor y este frontend ya no la calcula (antes había
// un cálculo provisional acá, `tarifa − saldo`, que se quitó al llegar el campo). Si el campo no
// llegara, devuelve 0 en vez de inventar un monto: la fila no ofrece cobro y se nota que falta el dato.
export function getAmountDue(item) {
  const fromServer = Number(item?.amountDue);
  return Number.isFinite(fromServer) ? Math.max(0, fromServer) : 0;
}

// Estado de la cuenta de la persona, TAL COMO lo entrega el servidor en cada fila de la cola
// (`accountStatus`, NOTAS-PARA-FRONTEND.md Bloque 4, punto 3.3): "DEBE" (saldo negativo, rojo),
// "VUELTO_PENDIENTE" (hay dinero suyo en la caja que se le debe devolver, azul) o "AL_DIA" (todo
// lo demás, incluido el saldo a favor por días adelantados). La regla vive en el backend y ya no se
// calcula acá: antes se comparaba `balance` con 0 y alguien con un día pagado por adelantado
// aparecía como "saldo pendiente" (docs/correciones 20-09.md).
//
// El único caso en que se mira `balance` es que el campo NO llegue (respuesta vieja o incompleta), y
// solo para no esconder una deuda: tratándose de dinero, un saldo negativo nunca debe verse "al día".
export function getAccountStatus(item) {
  if (item?.accountStatus) return item.accountStatus;
  return Number(item?.balance || 0) < 0 ? "DEBE" : "AL_DIA";
}

// Método de pago que queda preseleccionado en cualquier registro de dinero (cobro, adelanto,
// reembolso): EFECTIVO si está entre los activos, y si no, el primero. Recepción cobra en efectivo
// casi siempre, así que lo normal es no tocar el selector — solo se cambia cuando la persona paga
// con otra cosa (pedido del 19-09: "necesito que sea rápido, por default efectivo"). Se busca por
// nombre y sin distinguir mayúsculas porque administración los crea en mayúsculas ("EFECTIVO").
export function pickDefaultPaymentMethodId(methods) {
  if (!Array.isArray(methods) || methods.length === 0) return null;
  const cash = methods.find((method) => /efectivo/i.test(String(method?.name || "")));
  return (cash ?? methods[0]).id ?? null;
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
