import { Check, CalendarDays, RotateCcw } from "lucide-react";
import { formatCOP } from "../../../utils/currency";
import { getBalance, getRowStyle, getSourceLabel } from "../utils";

export default function QueueRow({ person, position, onAmountChange, onSettle, onConfirmar, onAnular, onOpenAdvance, onOpenRefund }) {
  const balance = getBalance(person);
  const rowStyle = getRowStyle(balance);

  return (
    <div className={`flex items-center gap-4 px-4 py-3 rounded-xl border transition-colors ${rowStyle}`}>
      <span className="w-6 text-neutral-500 text-sm font-semibold text-center">{position}</span>

      <div className="flex-1 min-w-0">
        <p className="text-white font-semibold truncate">{person.name}</p>
        <p className="text-xs font-medium text-neutral-500">{person.note || getSourceLabel(person.source)}</p>
      </div>

      <input
        type="text"
        inputMode="numeric"
        value={Number(person.amount || 0).toLocaleString("es-CO")}
        onChange={(e) => {
          const raw = e.target.value.replace(/\D/g, "");
          onAmountChange(raw === "" ? "0" : raw);
        }}
        className="w-32 bg-neutral-800 border border-neutral-700 rounded-lg px-3 py-2 text-white text-sm outline-none focus:border-emerald-500"
      />

      {/* Saldo — depende solo del monto, nunca de si está atendido */}
      <div className="w-36 flex flex-col items-end gap-0.5">
        {balance === 0 && <span className="text-emerald-400 text-sm font-semibold">Al día</span>}
        {balance < 0 && (
          <>
            <span className="text-red-400 text-sm font-semibold">Debe {formatCOP(Math.abs(balance))}</span>
            <button onClick={onSettle} className="text-xs text-neutral-500 hover:text-emerald-400 underline underline-offset-2">
              Completar pago
            </button>
          </>
        )}
        {balance > 0 && (
          <>
            <span className="text-sky-400 text-sm font-semibold">Vuelto {formatCOP(balance)}</span>
            <div className="flex gap-2">
              <button onClick={onSettle} className="text-xs text-neutral-500 hover:text-emerald-400 underline underline-offset-2">
                Entregar vuelto
              </button>
              <button onClick={onOpenRefund} className="text-xs text-neutral-500 hover:text-sky-400 underline underline-offset-2">
                Reembolsar
              </button>
            </div>
          </>
        )}
        {balance === 0 && Number(person.amount) > 0 && (
          <button onClick={onOpenRefund} className="text-xs text-neutral-600 hover:text-sky-400 underline underline-offset-2">
            Reembolsar
          </button>
        )}
      </div>

      {/* Asistencia — completamente separado del pago */}
      <div className="w-36 flex justify-end">
        {person.atendido ? (
          <div className="flex items-center gap-2">
            <span className="flex items-center gap-1 text-emerald-400 text-sm font-semibold">
              <Check size={16} />
              Atendido
            </span>
            <button
              onClick={onAnular}
              title="Anular confirmación"
              className="text-neutral-600 hover:text-red-400 p-1 rounded-md hover:bg-neutral-800 transition-colors"
            >
              <RotateCcw size={14} />
            </button>
          </div>
        ) : (
          <button
            onClick={onConfirmar}
            className="text-sm font-semibold px-3.5 py-2 rounded-lg bg-emerald-500 text-neutral-950 hover:bg-emerald-400 transition-colors"
          >
            Confirmar
          </button>
        )}
      </div>

      <div className="w-9 flex justify-end">
        {!person.atendido && (
          <button
            onClick={onOpenAdvance}
            title="Pago por varios días"
            className="text-neutral-500 hover:text-emerald-400 p-1.5 rounded-lg hover:bg-neutral-800 transition-colors"
          >
            <CalendarDays size={18} />
          </button>
        )}
      </div>
    </div>
  );
}
