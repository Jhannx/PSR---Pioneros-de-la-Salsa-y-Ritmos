import { useEffect, useState } from "react";
import { CheckCircle2 } from "lucide-react";
import { formatCOP } from "../../../utils/currency";
import { paymentsService } from "../../../services/paymentsService";

export default function PendingChangeTab() {
  const [pending, setPending] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deliveringId, setDeliveringId] = useState(null);

  useEffect(() => {
    async function load() {
      try {
        const data = await paymentsService.getPendingChanges();
        setPending(Array.isArray(data) ? data : []);
      } catch (err) {
        setError(err.data?.message || err.message || "No se pudo cargar los vueltos pendientes.");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  async function markDelivered(movementId) {
    setDeliveringId(movementId);
    setError("");
    try {
      await paymentsService.deliverChange(movementId);
      setPending((prev) => prev.filter((p) => p.movementId !== movementId));
    } catch (err) {
      setError(err.data?.message || err.message || "No se pudo entregar el vuelto.");
    } finally {
      setDeliveringId(null);
    }
  }

  if (loading) return <div className="text-center py-10 text-neutral-600 text-sm">Cargando vueltos pendientes…</div>;

  return (
    <div className="flex flex-col gap-2">
      {error && (
        <div className="p-4 bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm rounded-2xl mb-2">
          {error}
        </div>
      )}
      {pending.map((movement) => {
        const change = movement.change ?? movement.amountReceived - movement.amountApplied;
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
              disabled={deliveringId === movement.movementId}
              className="flex items-center gap-1.5 bg-white disabled:opacity-40 text-neutral-950 hover:bg-neutral-200 font-semibold text-sm px-3.5 py-2 rounded-xl transition-colors"
            >
              <CheckCircle2 size={14} />
              {deliveringId === movement.movementId ? "Entregando..." : "Entregado"}
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
