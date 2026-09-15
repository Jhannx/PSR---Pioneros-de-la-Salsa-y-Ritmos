import { useState } from "react";
import { Check, CalendarDays, RotateCcw } from "lucide-react";
import { formatCOP } from "../../../utils/currency";

// Rediseño del cobro (errores-13sep-front.md #1 / errores-13sep-compartido.md #1): antes,
// `item.balance` se sobreescribía localmente con un cálculo inventado (`monto tecleado - tarifa`)
// apenas alguien tocaba el campo — pisando el `saldo` REAL que manda el backend
// (`TodayQueueViewResponse.balance`, desde `vista_saldo_persona`: crédito de cuenta acumulado de
// toda la vida, `total_abonado - total_cobrado`, nada que ver con "hoy" solamente). Ahora el monto
// a cobrar es un estado puramente local, nunca toca `item.balance`, y el saldo mostrado es siempre
// el real. Cuando lo tecleado supera la tarifa del día, se pregunta con dos botones directos
// (decisión del 13-sep): "Entregar vuelto" (todo el excedente se devuelve) o "Abonar a sesiones
// futuras" (el excedente cubre tantos días completos como alcance; lo que sobra de ahí, que nunca
// llega a un día completo, también se entrega como vuelto — nunca queda pendiente).
export default function QueueRow({
  item,
  position,
  dailyRate = 0,
  sessionLabel,
  onCharge,
  onConfirmAttendance,
  onUndoAttendance,
  onOpenAdvance,
  onOpenRefund,
}) {
  const [amountInput, setAmountInput] = useState(null);
  const [charging, setCharging] = useState(false);
  const amount = amountInput ?? dailyRate;
  const hasExcess = amount > dailyRate;

  const balance = Number(item.balance || 0);
  const cardStyle =
    balance < 0
      ? "bg-red-950/40 border-red-500/40 text-red-100"
      : item.attended
        ? "bg-emerald-950/40 border-emerald-500/40 text-emerald-100"
        : "bg-neutral-900/90 border-neutral-800 text-neutral-200";

  const sourceLabel =
    item.checkinMethod === "QR" ? "QR" : item.checkinMethod === "MANUAL" ? "MANUAL" : "Terminal PSR";
  const fullName = `${item.firstName} ${item.lastName}`.trim();

  async function handleCharge(mode) {
    if (!onCharge || amount <= 0 || charging) return;
    setCharging(true);
    try {
      await onCharge(amount, mode);
      setAmountInput(null);
    } finally {
      setCharging(false);
    }
  }

  // Vista previa del reparto antes de que la persona en el mostrador haga clic — "la ui lo debe de
  // saber" (pedido explícito): que quede claro qué pasa con cada peso antes de confirmarlo, no
  // solo después.
  const daysIfAdvance = Math.max(1, Math.floor(amount / dailyRate));
  const appliedIfAdvance = Math.min(amount, daysIfAdvance * dailyRate);
  const changeIfAdvance = amount - appliedIfAdvance;
  const changeIfSimple = amount - Math.min(amount, dailyRate);

  return (
    <div
      className={`flex flex-col sm:flex-row items-start sm:items-center gap-3 px-4 py-3.5 rounded-2xl border transition-colors ${cardStyle}`}
    >
      {/* Posición en la fila */}
      <span className="w-6 text-neutral-400 text-sm font-bold text-center shrink-0">
        #{position}
      </span>

      {/* Nombre de la persona y fuente de registro */}
      <div className="flex-1 min-w-[220px] pr-2">
        <p className="text-white font-bold text-base md:text-lg tracking-tight" title={fullName}>
          {fullName}
        </p>
        <p className="text-xs font-semibold text-neutral-400 uppercase tracking-wider mt-0.5">
          {item.note || sourceLabel}
          {/* A qué sesión se le está cobrando — antes invisible (problemas-finales, 13-sep):
              "no sé qué estoy cobrando, ni a qué sesión le estoy asignando el pago". */}
          {sessionLabel && <span className="text-neutral-600 normal-case font-normal"> · {sessionLabel}</span>}
        </p>
      </div>

      {/* Columna: Monto a Cobrar */}
      <div className="w-full sm:w-40 shrink-0">
        <span className="text-[10px] text-neutral-400 font-semibold uppercase block sm:hidden mb-1">
          Monto a Cobrar
        </span>
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

        {!hasExcess ? (
          <button
            onClick={() => handleCharge("change")}
            disabled={charging || amount <= 0}
            className="w-full mt-1.5 text-xs font-bold px-3 py-1.5 rounded-lg bg-emerald-500 text-neutral-950 hover:bg-emerald-400 disabled:opacity-40 transition-colors"
          >
            {charging ? "Cobrando…" : "Cobrar"}
          </button>
        ) : (
          <div className="mt-1.5 flex flex-col gap-1">
            <button
              onClick={() => handleCharge("change")}
              disabled={charging}
              title={`Cubre el día de hoy (${formatCOP(dailyRate)}) y entrega ${formatCOP(changeIfSimple)} de vuelto`}
              className="w-full text-xs font-bold px-3 py-1.5 rounded-lg bg-sky-500 text-neutral-950 hover:bg-sky-400 disabled:opacity-40 transition-colors"
            >
              Entregar vuelto ({formatCOP(changeIfSimple)})
            </button>
            <button
              onClick={() => handleCharge("advance")}
              disabled={charging}
              title={`Cubre ${daysIfAdvance} día(s) y entrega ${formatCOP(changeIfAdvance)} de vuelto`}
              className="w-full text-xs font-bold px-3 py-1.5 rounded-lg bg-emerald-500 text-neutral-950 hover:bg-emerald-400 disabled:opacity-40 transition-colors"
            >
              Abonar {daysIfAdvance} día{daysIfAdvance > 1 ? "s" : ""}
              {changeIfAdvance > 0 ? ` (+${formatCOP(changeIfAdvance)} vuelto)` : ""}
            </button>
          </div>
        )}
      </div>

      {/* Columna: Saldo real de la cuenta */}
      <div className="w-full sm:w-40 flex flex-col items-start sm:items-end justify-center shrink-0">
        <span className="text-[10px] text-neutral-400 font-semibold uppercase block sm:hidden mb-1">
          Saldo
        </span>
        <span className={`text-sm font-bold ${balance < 0 ? "text-red-400" : "text-emerald-400"}`}>
          {balance < 0 ? `Debe ${formatCOP(Math.abs(balance))}` : `A favor ${formatCOP(balance)}`}
        </span>
        <button
          onClick={onOpenRefund}
          disabled={balance <= 0}
          title={balance <= 0 ? "No hay saldo a favor para reembolsar" : undefined}
          className="text-xs text-neutral-400 hover:text-sky-300 underline underline-offset-2 mt-0.5 disabled:opacity-30 disabled:no-underline disabled:cursor-not-allowed"
        >
          Reembolsar
        </button>
      </div>

      {/* Columna: Asistencia */}
      <div className="w-full sm:w-36 flex items-center justify-start sm:justify-end gap-2 shrink-0">
        {item.attended ? (
          <div className="flex items-center gap-2">
            <span className="flex items-center gap-1.5 text-emerald-400 text-sm font-bold bg-emerald-500/10 border border-emerald-500/30 px-3 py-1.5 rounded-full">
              <Check size={16} />
              Atendido
            </span>
            <button
              onClick={onUndoAttendance}
              title="Anular confirmación de asistencia"
              className="text-neutral-400 hover:text-red-400 p-1.5 rounded-xl hover:bg-neutral-800 transition-colors"
            >
              <RotateCcw size={16} />
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <button
              onClick={onConfirmAttendance}
              className="text-sm font-bold px-4 py-2 rounded-full bg-emerald-500 text-neutral-950 hover:bg-emerald-400 transition-colors shadow-md shadow-emerald-500/20"
            >
              Confirmar
            </button>
            <button
              onClick={onOpenAdvance}
              title="Pago por varios días por adelantado"
              className="text-neutral-400 hover:text-emerald-400 p-1.5 rounded-xl hover:bg-neutral-800 transition-colors"
            >
              <CalendarDays size={18} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
