import { useState } from "react";
import { History } from "lucide-react";

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function PricingTab() {
  const [amount, setAmount] = useState(4000);
  const [saved, setSaved] = useState(true);

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 max-w-sm">
      <label className="text-neutral-400 text-sm font-medium">Valor por sesión</label>
      <div className="flex items-center gap-2 mt-1.5">
        <span className="text-neutral-500 font-semibold">$</span>
        <input
          type="number"
          value={amount}
          onChange={(e) => {
            setAmount(e.target.value);
            setSaved(false);
          }}
          className={inputClass}
        />
      </div>

      <button
        onClick={() => setSaved(true)}
        disabled={saved}
        className="w-full mt-4 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
      >
        {saved ? "Guardado" : "Guardar cambios"}
      </button>

      <div className="mt-4 pt-4 border-t border-neutral-800 flex items-center gap-1.5 text-neutral-600 text-xs">
        <History size={13} />
        Última actualización: 27 ago 2026 · Camilo Pérez
      </div>
    </div>
  );
}
