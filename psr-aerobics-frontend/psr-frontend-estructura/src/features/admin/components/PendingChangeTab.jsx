import { useState } from "react";
import { CheckCircle2 } from "lucide-react";
import { formatCOP } from "../../../utils/currency";

const initialPendingChange = [
  { movementId: 120, fullName: "Laura Gómez Restrepo", amountReceived: 5000, amountApplied: 4000, dateTime: "2026-08-27T06:05:00" },
  { movementId: 118, fullName: "Camilo Pérez Londoño", amountReceived: 10000, amountApplied: 8000, dateTime: "2026-08-26T18:12:00" },
];

export default function PendingChangeTab() {
  const [pending, setPending] = useState(initialPendingChange);

  function markDelivered(id) {
    setPending((prev) => prev.filter((p) => p.movementId !== id));
  }

  return (
    <div className="flex flex-col gap-2">
      {pending.map((movement) => {
        const change = movement.amountReceived - movement.amountApplied;
        return (
          <div key={movement.movementId} className="flex items-center gap-4 px-4 py-3.5 bg-neutral-900 border border-neutral-800 rounded-2xl">
            <div className="flex-1">
              <p className="text-white font-semibold">{movement.fullName}</p>
              <p className="text-neutral-500 text-xs mt-0.5">
                Recibido {formatCOP(movement.amountReceived)} · Abonado {formatCOP(movement.amountApplied)}
              </p>
            </div>
            <span className="text-amber-400 font-bold">{formatCOP(change)}</span>
            <button
              onClick={() => markDelivered(movement.movementId)}
              className="flex items-center gap-1.5 bg-white text-neutral-950 hover:bg-neutral-200 font-semibold text-sm px-3.5 py-2 rounded-xl transition-colors"
            >
              <CheckCircle2 size={14} />
              Entregado
            </button>
          </div>
        );
      })}
      {pending.length === 0 && (
        <div className="text-center py-10 text-neutral-600 text-sm">No hay vueltos pendientes por entregar.</div>
      )}
    </div>
  );
}
