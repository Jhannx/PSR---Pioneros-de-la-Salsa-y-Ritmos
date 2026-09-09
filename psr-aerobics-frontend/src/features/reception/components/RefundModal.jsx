import { useState, useEffect } from "react";
import { X, Undo2 } from "lucide-react";
import { formatCOP } from "../../../utils/currency";
import { paymentsService } from "../../../services/paymentsService";

export default function RefundModal({ person = {}, onConfirm, onClose, error = "" }) {
  const max = Number(person.amount || person.amountCharged || person.balance || 4000);
  const [amount, setAmount] = useState(max);
  const [paymentMethods, setPaymentMethods] = useState([
    { id: 1, name: "Efectivo" },
    { id: 2, name: "Nequi / Bancolombia" },
  ]);
  const [selectedMethodId, setSelectedMethodId] = useState(1);

  useEffect(() => {
    async function loadMethods() {
      try {
        const methods = await paymentsService.getPaymentMethods();
        if (Array.isArray(methods) && methods.length > 0) {
          setPaymentMethods(methods);
          setSelectedMethodId(methods[0].id);
        }
      } catch (err) {
        console.warn("No se cargaron métodos de pago desde API, usando defaults:", err);
      }
    }
    loadMethods();
  }, []);

  const canConfirm = amount > 0 && amount <= max && selectedMethodId;

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
          {person.name || person.firstName || "Alumno"} · tiene abonado {formatCOP(max)}
        </p>

        <div className="space-y-3 mb-4">
          <div>
            <label className="text-neutral-400 text-xs font-medium">Método de devolución</label>
            <select
              value={selectedMethodId}
              onChange={(e) => setSelectedMethodId(Number(e.target.value))}
              className="w-full mt-1 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white text-sm outline-none focus:border-sky-500"
            >
              {paymentMethods.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="text-neutral-400 text-xs font-medium">Monto a devolver</label>
            <input
              type="text"
              inputMode="numeric"
              value={amount.toLocaleString("es-CO")}
              onChange={(e) => {
                const raw = e.target.value.replace(/\D/g, "");
                setAmount(Math.min(max, Number(raw || 0)));
              }}
              className="w-full mt-1 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white text-sm outline-none focus:border-sky-500"
            />
          </div>
        </div>

        {error && (
          <div className="mb-3 p-2.5 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl text-center">
            {error}
          </div>
        )}

        <button
          onClick={() => canConfirm && onConfirm(amount, selectedMethodId)}
          disabled={!canConfirm}
          className="w-full bg-sky-500 disabled:opacity-30 hover:bg-sky-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
        >
          Confirmar reembolso
        </button>
      </div>
    </div>
  );
}
