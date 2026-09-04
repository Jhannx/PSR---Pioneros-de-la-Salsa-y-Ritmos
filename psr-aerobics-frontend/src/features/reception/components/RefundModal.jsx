import { useState } from "react";
import { X, Undo2 } from "lucide-react";
import { formatCOP } from "../../../utils/currency";

// Reembolso real (sp_registrar_reembolso) — no puede superar lo que la
// persona ya tiene abonado.
export default function RefundModal({ person, onConfirm, onClose }) {
  const [amount, setAmount] = useState(0);
  const max = Number(person.amount || 0);
  const canConfirm = amount > 0 && amount <= max;

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50">
      <div className="bg-neutral-900 border border-sky-500/30 rounded-2xl p-6 w-full max-w-sm">
        <div className="flex items-center justify-between mb-1">
          <h2 className="text-white font-bold text-lg flex items-center gap-2">
            <Undo2 size={18} className="text-sky-400" />
            Reembolsar
          </h2>
          <button onClick={onClose} className="text-neutral-500 hover:text-white">
            <X size={20} />
          </button>
        </div>
        <p className="text-neutral-500 text-sm mb-4">
          {person.name} · tiene abonado {formatCOP(max)}
        </p>

        <label className="text-neutral-400 text-xs font-medium">Monto a devolver</label>
        <input
          type="text"
          inputMode="numeric"
          value={amount.toLocaleString("es-CO")}
          onChange={(e) => {
            const raw = e.target.value.replace(/\D/g, "");
            setAmount(Math.min(max, Number(raw || 0)));
          }}
          className="w-full mt-1 mb-4 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white text-sm outline-none focus:border-sky-500"
        />

        <button
          onClick={() => canConfirm && onConfirm(amount)}
          disabled={!canConfirm}
          className="w-full bg-sky-500 disabled:opacity-30 hover:bg-sky-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
        >
          Confirmar reembolso
        </button>
      </div>
    </div>
  );
}
