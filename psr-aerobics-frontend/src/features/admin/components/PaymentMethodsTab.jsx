import { useEffect, useState } from "react";
import { Plus, Power } from "lucide-react";
import { paymentsService } from "../../../services/paymentsService";

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function PaymentMethodsTab({ readOnly = false }) {
  const [methods, setMethods] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [newMethod, setNewMethod] = useState("");
  const [adding, setAdding] = useState(false);

  async function loadMethods() {
    setLoading(true);
    setError("");
    try {
      // Todos, activos e inactivos: esta pestaña es donde se activan y desactivan.
      const data = await paymentsService.getAllPaymentMethods();
      setMethods(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.data?.message || err.message || "No se pudo cargar los métodos de pago.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    Promise.resolve().then(loadMethods);
  }, []);

  async function addMethod() {
    if (!newMethod.trim()) return;
    setAdding(true);
    setError("");
    try {
      const created = await paymentsService.createPaymentMethod({ name: newMethod.trim().toUpperCase() });
      setMethods((prev) => [...prev, created]);
      setNewMethod("");
    } catch (err) {
      setError(err.data?.message || err.message || "No se pudo crear el método de pago.");
    } finally {
      setAdding(false);
    }
  }

  async function toggle(method) {
    setError("");
    try {
      const updated = await paymentsService.togglePaymentMethod(method.id, !method.active);
      setMethods((prev) => prev.map((m) => (m.id === method.id ? { ...m, active: updated?.active ?? !method.active } : m)));
    } catch (err) {
      setError(err.data?.message || err.message || "No se pudo actualizar el método de pago.");
    }
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 max-w-sm">
      {error && <p className="text-rose-400 text-xs mb-3">{error}</p>}

      <div className="flex flex-col gap-2 mb-4">
        {loading ? (
          <p className="text-neutral-500 text-sm">Cargando métodos de pago…</p>
        ) : methods.length === 0 ? (
          <p className="text-neutral-500 text-sm">No hay métodos de pago registrados.</p>
        ) : (
          methods.map((method) => (
            <div
              key={method.id}
              className="flex items-center justify-between bg-neutral-800/60 rounded-xl px-4 py-3"
            >
              <span className={`font-semibold text-sm ${method.active ? "text-white" : "text-neutral-600"}`}>
                {method.name}
              </span>
              {readOnly ? (
                <span
                  className={`flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full ${
                    method.active ? "bg-emerald-500/15 text-emerald-400" : "bg-neutral-800 text-neutral-500"
                  }`}
                >
                  <Power size={11} />
                  {method.active ? "Activo" : "Inactivo"}
                </span>
              ) : (
                <button
                  onClick={() => toggle(method)}
                  className={`flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full transition-colors ${
                    method.active ? "bg-emerald-500/15 text-emerald-400" : "bg-neutral-800 text-neutral-500"
                  }`}
                >
                  <Power size={11} />
                  {method.active ? "Activo" : "Inactivo"}
                </button>
              )}
            </div>
          ))
        )}
      </div>

      {!readOnly && (
        <div className="flex gap-2">
          <input
            value={newMethod}
            onChange={(e) => setNewMethod(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && addMethod()}
            placeholder="Ej. Nequi"
            disabled={adding}
            className={inputClass}
          />
          <button
            onClick={addMethod}
            disabled={adding || !newMethod.trim()}
            className="shrink-0 bg-white disabled:opacity-40 text-neutral-950 hover:bg-neutral-200 font-semibold px-3.5 rounded-xl transition-colors"
          >
            <Plus size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
