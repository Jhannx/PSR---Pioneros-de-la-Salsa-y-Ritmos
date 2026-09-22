import { useState, useEffect, useRef } from "react";
import { X, Undo2 } from "lucide-react";
import { formatCOP } from "../../../utils/currency";
import { paymentsService } from "../../../services/paymentsService";
import { pickDefaultPaymentMethodId } from "../utils";

export default function RefundModal({ person = {}, onConfirm, onClose, error = "" }) {
  // Antes: `person.amount || ... || 4000` — con un tope de 0 (nada que devolver) el `||` caía al
  // valor de respaldo 4000 y el modal dejaba "devolver" 4.000 que no existían. El único que abre
  // este modal (ReceptionCheckinPage) siempre manda `amount`, ya calculado con `getRefundCap`.
  const max = Math.max(0, Number(person.amount) || 0);
  const [amount, setAmount] = useState(max);
  // Sin lista de respaldo inventada (antes: Efectivo/Nequi con ids 1 y 2): si el servidor no
  // responde o no hay ningún método activo, mostrar esos dos igual permitía elegir métodos que
  // quizá ya estaban desactivados — justo lo que se reportó el 19-09. Solo se ofrece lo que el
  // servidor confirma como activo.
  const [paymentMethods, setPaymentMethods] = useState([]);
  const [selectedMethodId, setSelectedMethodId] = useState(null);
  const [methodsLoading, setMethodsLoading] = useState(true);
  const [methodsError, setMethodsError] = useState("");
  // Candado contra doble envío (docs/correcciones-pendientes-18-09.md #6): sin esto, dos clics
  // rápidos en "Confirmar reembolso" mandan dos POST — el primero devuelve el saldo completo, el
  // segundo llega con 0 disponible y el backend responde 400 ("la persona solo tiene 0 pendiente
  // de devolver"). Eso es justo lo que se vio: el back sí había ejecutado bien el primero, y el
  // error en pantalla venía del segundo envío fantasma.
  const [submitting, setSubmitting] = useState(false);
  const mountedRef = useRef(true);

  useEffect(() => {
    let cancelled = false;
    async function loadMethods() {
      try {
        // Solo los activos (ver `getActivePaymentMethods`), con efectivo preseleccionado.
        const active = await paymentsService.getActivePaymentMethods();
        if (cancelled) return;
        setPaymentMethods(active);
        setSelectedMethodId(pickDefaultPaymentMethodId(active));
      } catch (err) {
        if (!cancelled) {
          setMethodsError(err.data?.message || err.message || "No se pudieron cargar los métodos de pago.");
        }
      } finally {
        if (!cancelled) setMethodsLoading(false);
      }
    }
    loadMethods();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // El tope sale de la cola en vivo (ReceptionCheckinPage lo recalcula en cada render), así que
  // puede bajar mientras el modal está abierto; `amount` es lo último que se tecleó y podría
  // quedar por encima. Se recorta al mostrar y al enviar, en vez de dejar el botón apagado sin
  // explicación.
  const safeAmount = Math.min(amount, max);
  const canConfirm = safeAmount > 0 && Boolean(selectedMethodId) && !submitting;

  // `onConfirm` (handleConfirmRefund del padre) hoy nunca rechaza — captura sus propios errores y
  // solo actualiza `error` — así que no se puede distinguir éxito de fallo por try/catch acá. Si
  // salió bien, el padre cierra el modal (`setRefundCheckinId(null)`) y este componente se
  // desmonta antes de que importe resetear `submitting`; si falló, el modal sigue montado con el
  // nuevo `error` visible y hay que volver a habilitar el botón. `mountedRef` evita el warning de
  // "estado actualizado en componente desmontado" en el primer caso.
  async function handleSubmit() {
    if (!canConfirm) return;
    setSubmitting(true);
    try {
      await onConfirm(safeAmount, selectedMethodId);
    } finally {
      if (mountedRef.current) setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50">
      <div className="bg-neutral-900 border border-sky-500/30 rounded-2xl p-6 w-full max-w-sm">
        <div className="flex items-center justify-between mb-1">
          <h2 className="text-white font-bold text-lg flex items-center gap-2">
            <Undo2 size={18} className="text-sky-400" />
            Reembolsar
          </h2>
          {/* Deshabilitado mientras hay una petición en vuelo: cerrar a mitad de un reembolso no
              lo cancela (ya salió el POST), solo confundiría sobre si quedó pendiente o no. */}
          <button
            onClick={onClose}
            disabled={submitting}
            className="text-neutral-500 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <X size={20} />
          </button>
        </div>
        <p className="text-neutral-500 text-sm mb-4">
          {person.name || person.firstName || "Alumno"} · máximo a devolver {formatCOP(max)}
        </p>

        <div className="space-y-3 mb-4">
          <div>
            <label className="text-neutral-400 text-xs font-medium">Método de devolución</label>
            <select
              value={selectedMethodId ?? ""}
              onChange={(e) => setSelectedMethodId(Number(e.target.value))}
              disabled={submitting || methodsLoading || paymentMethods.length === 0}
              className="w-full mt-1 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white text-sm outline-none focus:border-sky-500 disabled:opacity-50"
            >
              {methodsLoading && <option value="">Cargando…</option>}
              {!methodsLoading && paymentMethods.length === 0 && <option value="">Sin métodos activos</option>}
              {paymentMethods.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name}
                </option>
              ))}
            </select>
            {!methodsLoading && (methodsError || paymentMethods.length === 0) && (
              <p className="text-amber-400 text-[11px] mt-1">
                {methodsError ||
                  "No hay ningún método de pago activo. Pide a un administrador que active al menos uno."}
              </p>
            )}
          </div>

          <div>
            <label className="text-neutral-400 text-xs font-medium">Monto a devolver</label>
            <input
              type="text"
              inputMode="numeric"
              value={safeAmount.toLocaleString("es-CO")}
              disabled={submitting}
              onChange={(e) => {
                const raw = e.target.value.replace(/\D/g, "");
                setAmount(Math.min(max, Number(raw || 0)));
              }}
              className="w-full mt-1 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white text-sm outline-none focus:border-sky-500 disabled:opacity-50"
            />
          </div>
        </div>

        {error && (
          <div className="mb-3 p-2.5 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl text-center">
            {error}
          </div>
        )}

        <button
          onClick={handleSubmit}
          disabled={!canConfirm}
          className="w-full bg-sky-500 disabled:opacity-30 hover:bg-sky-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
        >
          {submitting ? "Procesando…" : "Confirmar reembolso"}
        </button>
      </div>
    </div>
  );
}
