import { useState } from "react";
import { Check, CalendarDays, RotateCcw } from "lucide-react";
import { formatCOP } from "../../../utils/currency";
import { getRefundCap, getAccountStatus, getAmountDue } from "../utils";

// Origen del check-in, tal como lo reporta el backend (`checkinMethod`, NOTAS-PARA-FRONTEND.md
// Bloque 4, punto 2). En el JSON viaja el nombre de la constante, no el valor en español de la base:
// `SELF` es el que marcó la propia persona desde su pantalla (el servidor no puede distinguir si
// llegó por el QR o abriendo la app, así que "propio" es lo único que consta). Un valor que no se
// conozca se muestra tal cual en vez de disfrazarse de otro origen — antes cualquier valor
// desconocido salía como "Terminal PSR" (docs/correciones 20-09.md).
const SOURCE_LABELS = { QR: "QR", MANUAL: "Manual", DOCUMENT: "Terminal PSR", SELF: "App / miembro" };

// Fila de la cola de recepción. Cobrar y confirmar la asistencia son cosas distintas:
//
// - Lo que se RECIBE se registra con "Cobrar" (POST /api/movimientos). El monto recibido es un
//   estado local de la fila y nunca pisa el saldo real de la cuenta, que sale del backend.
// - Lo que la persona tiene que pagar AHORA (`dueAmount`) es el `amountDue` que entrega el servidor
//   (deuda anterior y saldo a favor ya descontados). Lo recibido se aplica primero a eso; solo lo
//   que sobre es excedente. Antes todo lo que superaba la tarifa era "vuelto", y quien debía dinero
//   no podía saldar su deuda (docs/correciones 20-09.md, 2ª ronda).
// - Con `dueAmount === 0` no hay nada que cobrar: la sesión ya está pagada (por adelantado o por un
//   pago de hoy) y la persona puede pasar directo — se muestra así en vez de ofrecer cobrar otra vez.
// - Cuando lo recibido supera lo que debe hay tres salidas directas (decisión del 20-sep, v1):
//   "Entregar vuelto", "Abonar N sesiones" (el excedente cubre sesiones completas por adelantado; lo que no
//   alcance a un día se entrega) y "Dejar vuelto pendiente" (el resto queda como dinero que se le debe
//   a la persona — para cuando no hay efectivo para el cambio de un billete de 50k o 100k).
//   Repartir el dinero en partes queda para v2 (docs/mejoras-v2.md #9).
// - La fila muestra cuánto se recibió en ESE check-in (`receivedAmount`).
// - El estado de la cuenta (rojo / azul / normal) lo decide el servidor (`accountStatus`).
export default function QueueRow({
  item,
  position,
  dailyRate = 0,
  sessionLabel,
  paymentMethods = [],
  defaultMethodId = null,
  pendingBusy = false,
  onCharge,
  onConfirmAttendance,
  onUndoAttendance,
  onOpenAdvance,
  onOpenRefund,
  onDeliverPending,
  onApplyPending,
}) {
  const [amountInput, setAmountInput] = useState(null);
  // `null` = el método por defecto (efectivo). Solo se toca cuando la persona paga con otra cosa, y
  // vuelve solo a efectivo después de cada cobro — igual que `amountInput` vuelve a lo que debe.
  const [methodId, setMethodId] = useState(null);
  const [charging, setCharging] = useState(false);

  const received = Number(item.receivedAmount || 0);
  const applied = Number(item.appliedAmount || 0);
  // Estado de la sesión de esta fila, según el reloj del servidor (`sessionStatus`): en una sesión
  // terminada ya no se cobra — los cobros "a partir de una sesión que ya se acabó" son justo lo
  // que se reportó.
  const sessionEnded = item.sessionStatus === "ENDED";

  const balance = Number(item.balance || 0);
  const dueAmount = getAmountDue(item);
  const debt = balance < 0 ? -balance : 0;
  const chargeVisible = !sessionEnded && dueAmount > 0;
  // Nada que cobrar: la tarifa de hoy ya está cubierta. `coveredByCredit` (servidor) dice que es por
  // sesiones pagadas por adelantado; si no, es que ya se pagó hoy.
  const coveredNow = !sessionEnded && dueAmount === 0;
  const coveredByCredit = Boolean(item.coveredByCredit);
  // Pagó una parte y aún debe la sesión: el descuento de la tarifa se hace al CONFIRMAR la asistencia,
  // así que hasta entonces el saldo de la cuenta muestra lo abonado "a favor" aunque falte pagar.
  // Se muestra lo que falta (`amountDue` del servidor) para que no parezca una sesión gratis.
  const partiallyPaid = !item.attended && !sessionEnded && dueAmount > 0 && balance > 0;

  // Lo que se teclea arranca en lo que debe pagar: con un clic exacto se salda todo.
  const amount = amountInput ?? dueAmount;
  const excess = Math.max(0, amount - dueAmount);
  const hasExcess = excess > 0;
  const shortfall = Math.max(0, dueAmount - amount);
  // Días completos que cubre el excedente si se abona por adelantado.
  const extraDays = dailyRate > 0 ? Math.floor(excess / dailyRate) : 0;
  const appliedIfAdvance = Math.min(amount, dueAmount + extraDays * dailyRate);
  const changeIfAdvance = amount - appliedIfAdvance;

  const selectedMethodId = methodId ?? defaultMethodId;
  const noMethod = !selectedMethodId;

  const status = getAccountStatus(item);
  const pendingChange = Number(item.pendingChange || 0);
  // `refundable` del backend, ya acotado al saldo a favor (ver `getRefundCap`). No es lo mismo que
  // `balance`, que es lo que se muestra como "A favor / Debe".
  const refundCap = getRefundCap(item);
  // Cuántos días completos cubre el vuelto pendiente si se convierte en abono.
  const daysFromPending = dailyRate > 0 ? Math.floor(pendingChange / dailyRate) : 0;

  const cardStyle =
    status === "DEBE"
      ? "bg-red-950/40 border-red-500/40 text-red-100"
      : status === "VUELTO_PENDIENTE"
        ? "bg-sky-950/40 border-sky-500/40 text-sky-100"
        : item.attended
          ? "bg-emerald-950/40 border-emerald-500/40 text-emerald-100"
          : "bg-neutral-900/90 border-neutral-800 text-neutral-200";

  const sourceLabel = SOURCE_LABELS[item.checkinMethod] ?? item.checkinMethod ?? "—";
  const fullName = `${item.firstName} ${item.lastName}`.trim();

  async function handleCharge(mode) {
    if (!onCharge || amount <= 0 || charging || noMethod) return;
    setCharging(true);
    try {
      await onCharge(amount, mode, selectedMethodId);
      setAmountInput(null);
      setMethodId(null);
    } finally {
      setCharging(false);
    }
  }

  return (
    <div
      // Quiebre en `lg` (1024px), no `sm` (640px) — ver el comentario del encabezado de columnas
      // en ReceptionCheckinPage.jsx (docs/correcciones-pendientes-18-09.md #5): en fila, las cinco
      // columnas necesitan ~738px mínimos, y entre 640-1024px (sidebar de 240px desde `md`) el
      // contenido no alcanza a tenerlos.
      className={`flex flex-col lg:flex-row items-start lg:items-center gap-3 px-4 py-3.5 rounded-2xl border transition-colors ${cardStyle}`}
    >
      {/* Posición en la fila */}
      <span className="w-6 text-neutral-400 text-sm font-bold text-center shrink-0">
        #{position}
      </span>

      {/* Nombre de la persona y fuente de registro. `min-w-0` (no un `min-w-[220px]` fijo) para
          que `truncate` funcione dentro de un flex row: sin él, un flex-1 nunca se encoge más
          allá del contenido y el nombre largo empuja a las demás columnas fuera de pantalla. */}
      <div className="flex-1 min-w-0 pr-2">
        <p className="text-white font-bold text-base md:text-lg tracking-tight truncate" title={fullName}>
          {fullName}
        </p>
        <p className="text-xs font-semibold text-neutral-400 uppercase tracking-wider mt-0.5">
          {/* `item.note` vuelve a usarse (docs/correcciones-pendientes-18-09.md #6, resuelto en
              NOTAS-PARA-FRONTEND.md Bloque 2, A4): el reembolso hereda el `llegada_id` del abono
              original, así que refleja lo último que pasó. */}
          {item.note || sourceLabel}
          {/* Origen del check-in, además de la nota, cuando la nota lo tapa. */}
          {item.note && <span className="text-neutral-600 normal-case font-normal"> · {sourceLabel}</span>}
          {/* A qué sesión se le está cobrando — antes invisible (problemas-finales, 13-sep):
              "no sé qué estoy cobrando, ni a qué sesión le estoy asignando el pago". */}
          {sessionLabel && <span className="text-neutral-600 normal-case font-normal"> · {sessionLabel}</span>}
        </p>
        {sessionEnded && (
          <p className="text-[11px] font-semibold text-amber-400 mt-1">Sesión terminada — ya no se cobra en ella</p>
        )}
      </div>

      {/* Columna: Monto recibido — lo que la persona entrega en el mostrador (no lo que se le
          cobra: se aplica a lo que debe y a la tarifa de hoy, y lo que sobre se abona, se entrega
          o queda pendiente). */}
      <div className="w-full lg:w-40 shrink-0">
        <span className="text-[10px] text-neutral-400 font-semibold uppercase block lg:hidden mb-1">
          Monto recibido
        </span>

        {/* Lo ya recibido en este check-in, siempre visible (viene del servidor). */}
        {received > 0 && (
          <div className="rounded-lg bg-emerald-500/10 border border-emerald-500/30 px-2.5 py-1.5 mb-1.5">
            <p className="text-emerald-400 text-xs font-bold">Recibido {formatCOP(received)}</p>
            <p className="text-neutral-400 text-[10px]">Aplicado {formatCOP(applied)}</p>
          </div>
        )}

        {chargeVisible && (
          <>
            {/* Lo que tiene que pagar ahora, y por qué — es el número al que apunta el cobro. */}
            <p className="text-[11px] text-neutral-400 mb-1">
              Por pagar: <span className="text-white font-bold">{formatCOP(dueAmount)}</span>
              {debt > 0 && (
                <span className="block text-red-300/90">
                  {item.attended ? "Deuda" : "Incluye deuda de"} {formatCOP(debt)}
                </span>
              )}
            </p>

            <input
              type="text"
              inputMode="numeric"
              disabled={charging}
              value={amount.toLocaleString("es-CO")}
              onChange={(e) => {
                const raw = e.target.value.replace(/\D/g, "");
                setAmountInput(raw === "" ? 0 : Number(raw));
              }}
              className="w-full bg-neutral-900/90 border border-neutral-700/80 rounded-xl px-3 py-2 text-white text-sm outline-none focus:border-emerald-500 font-semibold"
            />

            {/* Método de pago de ESTE cobro — todo registro de dinero lleva el suyo. Preseleccionado
                en efectivo para que en el caso normal no haya que tocar nada. */}
            <select
              value={selectedMethodId ?? ""}
              onChange={(e) => setMethodId(Number(e.target.value))}
              disabled={charging || paymentMethods.length === 0}
              aria-label="Método de pago"
              title="Método de pago"
              className="w-full mt-1.5 bg-neutral-900/90 border border-neutral-700/80 rounded-lg px-2 py-1 text-neutral-300 text-xs outline-none focus:border-emerald-500 disabled:opacity-50"
            >
              {paymentMethods.length === 0 && <option value="">Sin método activo</option>}
              {paymentMethods.map((method) => (
                <option key={method.id} value={method.id}>
                  {method.name}
                </option>
              ))}
            </select>

            {!hasExcess ? (
              <>
                <button
                  onClick={() => handleCharge("change")}
                  disabled={charging || amount <= 0 || noMethod}
                  className="w-full mt-1.5 text-xs font-bold px-3 py-1.5 rounded-lg bg-emerald-500 text-neutral-950 hover:bg-emerald-400 disabled:opacity-40 transition-colors"
                >
                  {charging ? "Cobrando…" : shortfall > 0 ? "Cobrar parcial" : "Cobrar"}
                </button>
                {shortfall > 0 && amount > 0 && (
                  <p className="text-[11px] text-amber-400 mt-1">Quedará debiendo {formatCOP(shortfall)}</p>
                )}
              </>
            ) : (
              <div className="mt-1.5 flex flex-col gap-1">
                <p className="text-[11px] text-sky-300">Sobran {formatCOP(excess)} — ¿qué se hace con ellos?</p>
                <button
                  onClick={() => handleCharge("change")}
                  disabled={charging || noMethod}
                  title={`Cubre ${formatCOP(dueAmount)} y entrega ${formatCOP(excess)} de vuelto ahora`}
                  className="w-full text-xs font-bold px-3 py-1.5 rounded-lg bg-sky-500 text-neutral-950 hover:bg-sky-400 disabled:opacity-40 transition-colors"
                >
                  Entregar vuelto ({formatCOP(excess)})
                </button>
                {extraDays >= 1 && (
                  <button
                    onClick={() => handleCharge("advance")}
                    disabled={charging || noMethod}
                    title={`Cubre ${formatCOP(dueAmount)} y abona ${extraDays} sesión(es) por adelantado`}
                    className="w-full text-xs font-bold px-3 py-1.5 rounded-lg bg-emerald-500 text-neutral-950 hover:bg-emerald-400 disabled:opacity-40 transition-colors"
                  >
                    Abonar {extraDays} sesión{extraDays > 1 ? "es" : ""}
                    {changeIfAdvance > 0 ? ` (+${formatCOP(changeIfAdvance)} vuelto)` : ""}
                  </button>
                )}
                {/* Para cuando no hay efectivo para el vuelto: se cubre lo que debe y el resto queda
                    como dinero que se le debe a la persona, sin detener la fila. */}
                <button
                  onClick={() => handleCharge("pending")}
                  disabled={charging || noMethod}
                  title={`Cubre ${formatCOP(dueAmount)} y deja ${formatCOP(excess)} pendientes de devolver`}
                  className="w-full text-xs font-bold px-3 py-1.5 rounded-lg border border-sky-400 text-sky-300 hover:bg-sky-500/10 disabled:opacity-40 transition-colors"
                >
                  Dejar vuelto pendiente ({formatCOP(excess)})
                </button>
              </div>
            )}
          </>
        )}

        {/* Nada que cobrar: la tarifa de hoy ya está cubierta. */}
        {coveredNow && (
          <div className="rounded-lg bg-emerald-500/10 border border-emerald-500/30 px-2.5 py-2">
            <p className="text-emerald-400 text-xs font-bold">
              {coveredByCredit ? "Sesión pagada por adelantado" : received > 0 ? "Sesión pagada" : "Al día"}
            </p>
            {!item.attended && (
              <p className="text-emerald-200/80 text-[11px] mt-0.5">Puede pasar directo a la clase</p>
            )}
            <button
              onClick={onOpenAdvance}
              className="mt-1 text-[11px] text-neutral-400 hover:text-emerald-300 underline underline-offset-2"
            >
              Pagar sesiones por adelantado
            </button>
          </div>
        )}
      </div>

      {/* Columna: Saldo — el estado de la cuenta lo decide el servidor (`accountStatus`). */}
      <div className="w-full lg:w-40 flex flex-col items-start lg:items-end justify-center shrink-0">
        <span className="text-[10px] text-neutral-400 font-semibold uppercase block lg:hidden mb-1">
          Saldo
        </span>
        {partiallyPaid && (
          <>
            <span className="text-sm font-bold text-amber-400">Falta {formatCOP(dueAmount)}</span>
            <span className="text-[10px] text-neutral-400 mt-0.5 lg:text-right">
              Tiene {formatCOP(balance)} a favor · la sesión se descuenta al confirmar
            </span>
          </>
        )}
        {!partiallyPaid && status === "DEBE" && (
          <span className="text-sm font-bold text-red-400">Debe {formatCOP(Math.abs(balance))}</span>
        )}
        {!partiallyPaid && status === "VUELTO_PENDIENTE" && (
          <span className="text-sm font-bold text-sky-400">Vuelto por entregar {formatCOP(pendingChange)}</span>
        )}
        {!partiallyPaid && status !== "DEBE" && status !== "VUELTO_PENDIENTE" && (
          <span className="text-sm font-bold text-emerald-400">
            {balance > 0 ? `A favor ${formatCOP(balance)}` : "Al día"}
          </span>
        )}

        {/* Vuelto pendiente: se puede entregar o convertir en sesiones abonadas con el mismo dinero. */}
        {status === "VUELTO_PENDIENTE" && (
          <div className="mt-1.5 flex flex-col gap-1 w-full lg:items-end">
            <button
              onClick={onDeliverPending}
              disabled={pendingBusy}
              className="w-full lg:w-auto text-xs font-bold px-3 py-1.5 rounded-lg bg-sky-500 text-neutral-950 hover:bg-sky-400 disabled:opacity-40 transition-colors"
            >
              Entregar vuelto
            </button>
            <button
              onClick={() => onApplyPending?.(daysFromPending)}
              disabled={pendingBusy || daysFromPending < 1}
              title={
                daysFromPending < 1
                  ? `Lo pendiente no alcanza para una sesión (${formatCOP(dailyRate)})`
                  : `Convierte ${formatCOP(daysFromPending * dailyRate)} del vuelto en ${daysFromPending} sesión(es) abonadas`
              }
              className="w-full lg:w-auto text-xs font-bold px-3 py-1.5 rounded-lg border border-sky-400 text-sky-300 hover:bg-sky-500/10 disabled:opacity-40 transition-colors"
            >
              Abonar {daysFromPending} sesión{daysFromPending === 1 ? "" : "es"}
            </button>
          </div>
        )}

        <button
          onClick={onOpenRefund}
          disabled={refundCap <= 0}
          title={refundCap <= 0 ? "No hay saldo a favor para devolver" : undefined}
          className="text-xs text-neutral-400 hover:text-sky-300 underline underline-offset-2 mt-1 disabled:opacity-30 disabled:no-underline disabled:cursor-not-allowed"
        >
          Reembolsar
        </button>
      </div>

      {/* Columna: Estado — atendido o todavía sin atender */}
      <div className="w-full lg:w-36 flex items-center justify-start lg:justify-end gap-2 shrink-0">
        {item.attended ? (
          <div className="w-full lg:w-auto flex items-center gap-2">
            <span className="flex items-center gap-1.5 text-emerald-400 text-sm font-bold bg-emerald-500/10 border border-emerald-500/30 px-3 py-1.5 rounded-full">
              <Check size={16} />
              Atendido
            </span>
            <button
              onClick={onUndoAttendance}
              title="Anular confirmación de asistencia"
              className="text-neutral-400 hover:text-red-400 p-1.5 rounded-xl hover:bg-neutral-800 transition-colors shrink-0"
            >
              <RotateCcw size={16} />
            </button>
          </div>
        ) : (
          // `flex-1` en el botón principal (solo hasta `lg`) — en la tarjeta apilada quedaba
          // apretado al ancho de su propio texto, con todo el resto de la fila vacío al lado
          // (docs/correcciones-pendientes-18-09.md #5).
          <div className="w-full lg:w-auto flex flex-col items-stretch lg:items-end gap-1.5">
            <span className="text-[10px] font-bold uppercase tracking-wider text-amber-400">Sin atender</span>
            <div className="flex items-center gap-2">
              <button
                onClick={onConfirmAttendance}
                className="flex-1 lg:flex-none text-sm font-bold px-4 py-2 rounded-full bg-emerald-500 text-neutral-950 hover:bg-emerald-400 transition-colors shadow-md shadow-emerald-500/20"
              >
                Confirmar
              </button>
              <button
                onClick={onOpenAdvance}
                disabled={sessionEnded}
                title={sessionEnded ? "La sesión ya terminó" : "Pago de varias sesiones por adelantado"}
                className="text-neutral-400 hover:text-emerald-400 p-1.5 rounded-xl hover:bg-neutral-800 transition-colors shrink-0 disabled:opacity-30 disabled:cursor-not-allowed"
              >
                <CalendarDays size={18} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
