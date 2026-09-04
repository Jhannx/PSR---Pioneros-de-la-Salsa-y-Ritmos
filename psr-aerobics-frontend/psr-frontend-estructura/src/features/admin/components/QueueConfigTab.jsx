import { useState } from "react";

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function QueueConfigTab() {
  const [peoplePerTurn, setPeoplePerTurn] = useState(1);
  const [marginMinutes, setMarginMinutes] = useState(60);
  const [visibilityMinutes, setVisibilityMinutes] = useState(2);

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 max-w-sm flex flex-col gap-4">
      <div>
        <label className="text-neutral-400 text-sm font-medium">Personas atendidas por turno</label>
        <input
          type="number"
          min={1}
          value={peoplePerTurn}
          onChange={(e) => setPeoplePerTurn(e.target.value)}
          className={`${inputClass} mt-1.5`}
        />
      </div>
      <div>
        <label className="text-neutral-400 text-sm font-medium">Margen para hacer check-in (minutos antes)</label>
        <input
          type="number"
          min={0}
          value={marginMinutes}
          onChange={(e) => setMarginMinutes(e.target.value)}
          className={`${inputClass} mt-1.5`}
        />
      </div>
      <div>
        <label className="text-neutral-400 text-sm font-medium">Visibilidad de un pago reciente (minutos)</label>
        <input
          type="number"
          min={0}
          value={visibilityMinutes}
          onChange={(e) => setVisibilityMinutes(e.target.value)}
          className={`${inputClass} mt-1.5`}
        />
      </div>

      <button className="mt-1 w-full bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors">
        Guardar cambios
      </button>
    </div>
  );
}
