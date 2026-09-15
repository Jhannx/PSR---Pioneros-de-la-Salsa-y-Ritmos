import { Check, CalendarDays, RotateCcw } from "lucide-react";
import { formatCOP } from "../../../utils/currency";

export default function QueueRow({
  item,
  position,
  onAmountChange,
  onSettle,
  onDeliverChange,
  onConfirmAttendance,
  onUndoAttendance,
  onOpenAdvance,
  onOpenRefund,
}) {
  const balance = Number(item.balance || 0);

  // Definición de estilos por tarjeta completa según el estado del saldo (Mockup 3.PNG)
  let cardStyle = "bg-neutral-900/90 border-neutral-800 text-neutral-200";
  if (balance < 0) {
    cardStyle = "bg-red-950/40 border-red-500/40 text-red-100";
  } else if (balance > 0) {
    cardStyle = "bg-blue-950/40 border-blue-500/40 text-blue-100";
  } else if (item.attended) {
    cardStyle = "bg-emerald-950/40 border-emerald-500/40 text-emerald-100";
  }

  const sourceLabel =
    item.checkinMethod === "QR" ? "QR" : item.checkinMethod === "MANUAL" ? "MANUAL" : "Terminal PSR";
  const fullName = `${item.firstName} ${item.lastName}`.trim();

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
        </p>
      </div>

      {/* Columna: Monto Pagado */}
      <div className="w-full sm:w-32 shrink-0">
        <span className="text-[10px] text-neutral-400 font-semibold uppercase block sm:hidden mb-1">
          Monto Pagado
        </span>
        <input
          type="text"
          inputMode="numeric"
          value={Number(item.amountCharged || 0).toLocaleString("es-CO")}
          onChange={(e) => {
            const raw = e.target.value.replace(/\D/g, "");
            onAmountChange(raw === "" ? 0 : Number(raw));
          }}
          className="w-full bg-neutral-900/90 border border-neutral-700/80 rounded-xl px-3 py-2 text-white text-sm outline-none focus:border-emerald-500 font-semibold"
        />
      </div>

      {/* Columna: Saldo */}
      <div className="w-full sm:w-40 flex flex-col items-start sm:items-end justify-center shrink-0">
        <span className="text-[10px] text-neutral-400 font-semibold uppercase block sm:hidden mb-1">
          Saldo
        </span>
        {balance === 0 && (
          <div className="flex flex-col items-start sm:items-end">
            <span className="text-emerald-400 text-sm font-bold">Al día</span>
            <button
              onClick={onOpenRefund}
              className="text-xs text-neutral-400 hover:text-sky-300 underline underline-offset-2 mt-0.5"
            >
              Reembolsar
            </button>
          </div>
        )}

        {balance < 0 && (
          <div className="flex flex-col items-start sm:items-end">
            <span className="text-red-400 text-sm font-bold">
              Debe {formatCOP(Math.abs(balance))}
            </span>
            <button
              onClick={onSettle}
              className="text-xs text-neutral-300 hover:text-emerald-400 underline underline-offset-2 mt-0.5"
            >
              Completar pago
            </button>
          </div>
        )}

        {balance > 0 && (
          <div className="flex flex-col items-start sm:items-end">
            <span className="text-sky-400 text-sm font-bold">
              Vuelto {formatCOP(balance)}
            </span>
            <div className="flex gap-2 mt-0.5">
              <button
                onClick={onDeliverChange}
                className="text-xs text-neutral-300 hover:text-emerald-400 underline underline-offset-2"
              >
                Entregar vuelto
              </button>
              <button
                onClick={onOpenRefund}
                className="text-xs text-neutral-300 hover:text-sky-300 underline underline-offset-2"
              >
                Reembolsar
              </button>
            </div>
          </div>
        )}
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
