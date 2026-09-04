import { useState } from "react";
import { Plus, Power } from "lucide-react";

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function PaymentMethodsTab() {
  const [methods, setMethods] = useState([
    { id: 1, name: "EFECTIVO", active: true },
    { id: 2, name: "TRANSFERENCIA", active: true },
  ]);
  const [newMethod, setNewMethod] = useState("");

  function addMethod() {
    if (!newMethod.trim()) return;
    setMethods((prev) => [...prev, { id: Date.now(), name: newMethod.trim().toUpperCase(), active: true }]);
    setNewMethod("");
  }

  function toggle(id) {
    setMethods((prev) => prev.map((m) => (m.id === id ? { ...m, active: !m.active } : m)));
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 max-w-sm">
      <div className="flex flex-col gap-2 mb-4">
        {methods.map((method) => (
          <div
            key={method.id}
            className="flex items-center justify-between bg-neutral-800/60 rounded-xl px-4 py-3"
          >
            <span className={`font-semibold text-sm ${method.active ? "text-white" : "text-neutral-600"}`}>
              {method.name}
            </span>
            <button
              onClick={() => toggle(method.id)}
              className={`flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full transition-colors ${
                method.active ? "bg-emerald-500/15 text-emerald-400" : "bg-neutral-800 text-neutral-500"
              }`}
            >
              <Power size={11} />
              {method.active ? "Activo" : "Inactivo"}
            </button>
          </div>
        ))}
      </div>

      <div className="flex gap-2">
        <input
          value={newMethod}
          onChange={(e) => setNewMethod(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && addMethod()}
          placeholder="Ej. Nequi"
          className={inputClass}
        />
        <button
          onClick={addMethod}
          className="shrink-0 bg-white text-neutral-950 hover:bg-neutral-200 font-semibold px-3.5 rounded-xl transition-colors"
        >
          <Plus size={16} />
        </button>
      </div>
    </div>
  );
}
