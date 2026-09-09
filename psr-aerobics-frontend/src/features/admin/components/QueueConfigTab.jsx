import { useEffect, useState } from "react";
import { queueService } from "../../../services/queueService";

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function QueueConfigTab({ readOnly = false }) {
  const [peoplePerTurn, setPeoplePerTurn] = useState("");
  const [entryMarginMinutes, setEntryMarginMinutes] = useState("");
  const [paymentVisibilityMinutes, setPaymentVisibilityMinutes] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        const data = await queueService.getConfig();
        setPeoplePerTurn(String(data?.peoplePerTurn ?? 1));
        setEntryMarginMinutes(String(data?.entryMarginMinutes ?? 0));
        setPaymentVisibilityMinutes(String(data?.paymentVisibilityMinutes ?? 1));
      } catch (err) {
        setError(err.data?.message || err.message || "No se pudo cargar la configuración de la cola.");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  async function handleSave() {
    setSaving(true);
    setError("");
    setSaved(false);
    try {
      await queueService.updateConfig({
        peoplePerTurn: Number(peoplePerTurn),
        entryMarginMinutes: Number(entryMarginMinutes),
        paymentVisibilityMinutes: Number(paymentVisibilityMinutes),
      });
      setSaved(true);
    } catch (err) {
      setError(err.data?.message || err.message || "No se pudo guardar la configuración.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 max-w-sm flex flex-col gap-4">
      <div>
        <label className="text-neutral-400 text-sm font-medium">Personas atendidas por turno</label>
        <input
          type="number"
          min={1}
          value={peoplePerTurn}
          disabled={loading || readOnly}
          onChange={(e) => {
            setPeoplePerTurn(e.target.value);
            setSaved(false);
          }}
          className={`${inputClass} mt-1.5`}
        />
      </div>
      <div>
        <label className="text-neutral-400 text-sm font-medium">Margen para hacer check-in (minutos antes)</label>
        <input
          type="number"
          min={0}
          value={entryMarginMinutes}
          disabled={loading || readOnly}
          onChange={(e) => {
            setEntryMarginMinutes(e.target.value);
            setSaved(false);
          }}
          className={`${inputClass} mt-1.5`}
        />
      </div>
      <div>
        <label className="text-neutral-400 text-sm font-medium">Visibilidad de un pago reciente (minutos)</label>
        <input
          type="number"
          min={0}
          value={paymentVisibilityMinutes}
          disabled={loading || readOnly}
          onChange={(e) => {
            setPaymentVisibilityMinutes(e.target.value);
            setSaved(false);
          }}
          className={`${inputClass} mt-1.5`}
        />
      </div>

      {error && <p className="text-rose-400 text-xs">{error}</p>}

      {!readOnly && (
        <button
          onClick={handleSave}
          disabled={saving || loading}
          className="mt-1 w-full bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
        >
          {saving ? "Guardando..." : saved ? "Guardado" : "Guardar cambios"}
        </button>
      )}
    </div>
  );
}
