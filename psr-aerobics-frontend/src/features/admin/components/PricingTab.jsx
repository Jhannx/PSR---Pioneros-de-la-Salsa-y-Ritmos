import { useEffect, useState } from "react";
import { History } from "lucide-react";
import { paymentsService } from "../../../services/paymentsService";
import { formatCOP } from "../../../utils/currency";

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function PricingTab({ readOnly = false }) {
  const [amount, setAmount] = useState("");
  const [current, setCurrent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        const data = await paymentsService.getPricing();
        setCurrent(data);
        setAmount(String(data?.amount ?? ""));
      } catch (err) {
        setError(err.data?.message || err.message || "No se pudo cargar la tarifa vigente.");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  async function handleSave() {
    setSaving(true);
    setError("");
    try {
      const updated = await paymentsService.updatePricing({ amount: Number(amount) });
      setCurrent(updated);
      setAmount(String(updated?.amount ?? amount));
      setDirty(false);
    } catch (err) {
      setError(err.data?.message || err.message || "No se pudo actualizar la tarifa.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 max-w-sm">
      <label className="text-neutral-400 text-sm font-medium">Valor por sesión</label>
      <div className="flex items-center gap-2 mt-1.5">
        <span className="text-neutral-500 font-semibold">$</span>
        <input
          type="number"
          value={amount}
          disabled={loading || readOnly}
          onChange={(e) => {
            setAmount(e.target.value);
            setDirty(true);
          }}
          className={inputClass}
        />
      </div>

      {error && <p className="text-rose-400 text-xs mt-3">{error}</p>}

      {!readOnly && (
        <button
          onClick={handleSave}
          disabled={!dirty || saving || loading || !amount}
          className="w-full mt-4 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
        >
          {saving ? "Guardando..." : dirty ? "Guardar cambios" : "Guardado"}
        </button>
      )}

      {current?.updatedAt && (
        <div className="mt-4 pt-4 border-t border-neutral-800 flex items-center gap-1.5 text-neutral-600 text-xs">
          <History size={13} />
          Última actualización: {new Date(current.updatedAt).toLocaleDateString("es-CO")}
          {current.updatedByName ? ` · ${current.updatedByName}` : ""}
        </div>
      )}

      {!loading && current && (
        <p className="text-neutral-600 text-[11px] mt-2">Tarifa vigente: {formatCOP(current.amount)}</p>
      )}
    </div>
  );
}
