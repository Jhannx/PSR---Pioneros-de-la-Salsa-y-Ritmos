import { useState } from "react";
import { Phone, User, MessageSquareText, Tags, Info } from "lucide-react";

// Herramienta de DECISIÓN para el dueño del negocio — NO está conectada
// a vista_registro_movil.jsx ni a vista_registro_kiosko.jsx. Reemplaza
// "Tipo de sangre" por datos que sí ayudan en una emergencia real: a
// quién llamar, y una nota corta de qué debería saber quien responda.
// Muestra ambas variantes de "observaciones" para comparar antes de
// decidir cuál va al formulario real.

const CONDITION_CHIPS = [
  "Asma",
  "Diabetes",
  "Alergias",
  "Problemas cardíacos",
  "Epilepsia",
  "Ninguna condición conocida",
];

const inputClass =
  "w-full bg-neutral-900 border border-neutral-800 rounded-xl px-4 py-3 text-white text-base placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function EmergencyContactExploration() {
  const [variant, setVariant] = useState("texto"); // texto | chips

  const [contactName, setContactName] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [freeText, setFreeText] = useState("");
  const [selectedChips, setSelectedChips] = useState([]);
  const [otherText, setOtherText] = useState("");

  function toggleChip(chip) {
    if (chip === "Ninguna condición conocida") {
      setSelectedChips((prev) => (prev.includes(chip) ? [] : [chip]));
      setOtherText("");
      return;
    }
    setSelectedChips((prev) => {
      const withoutNone = prev.filter((c) => c !== "Ninguna condición conocida");
      return withoutNone.includes(chip) ? withoutNone.filter((c) => c !== chip) : [...withoutNone, chip];
    });
  }

  const observationsPreview =
    variant === "texto"
      ? freeText.trim() || "(sin observaciones)"
      : [...selectedChips, otherText.trim()].filter(Boolean).join(", ") || "(sin observaciones)";

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-start justify-center px-4 py-10">
      <div className="w-full max-w-lg">
        <div className="text-center mb-6">
          <div className="inline-flex items-center gap-1.5 bg-neutral-900 border border-neutral-800 text-neutral-500 text-xs font-semibold px-3 py-1.5 rounded-full mb-3">
            <Info size={12} />
            Vista de exploración — aún no conectada al registro real
          </div>
          <h1 className="text-2xl font-bold text-white">Datos de emergencia</h1>
          <p className="text-neutral-400 text-sm mt-1">Reemplazo propuesto para "Tipo de sangre"</p>
        </div>

        <div className="bg-neutral-900 rounded-3xl p-6 border border-neutral-800 flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-3">
            <Field label="Nombre de contacto" icon={<User size={14} />}>
              <input
                value={contactName}
                onChange={(e) => setContactName(e.target.value)}
                placeholder="Ej. Rosa Martínez (mamá)"
                className={inputClass}
              />
            </Field>
            <Field label="Teléfono de contacto" icon={<Phone size={14} />}>
              <input
                type="tel"
                inputMode="tel"
                value={contactPhone}
                onChange={(e) => setContactPhone(e.target.value)}
                placeholder="300 987 6543"
                className={inputClass}
              />
            </Field>
          </div>

          <div className="border-t border-neutral-800 pt-4">
            <div className="flex items-center justify-between mb-3">
              <label className="text-neutral-300 text-sm font-semibold flex items-center gap-1.5">
                <MessageSquareText size={14} />
                Observaciones de emergencia
              </label>

              {/* Selector de variante — es lo que permite comparar */}
              <div className="flex bg-neutral-800 rounded-lg p-1 text-xs font-semibold">
                <button
                  onClick={() => setVariant("texto")}
                  className={`px-3 py-1.5 rounded-md transition-colors ${
                    variant === "texto" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400"
                  }`}
                >
                  Texto libre
                </button>
                <button
                  onClick={() => setVariant("chips")}
                  className={`px-3 py-1.5 rounded-md transition-colors ${
                    variant === "chips" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400"
                  }`}
                >
                  Lista de chips
                </button>
              </div>
            </div>

            {variant === "texto" ? (
              <div>
                <textarea
                  value={freeText}
                  onChange={(e) => setFreeText(e.target.value)}
                  placeholder="Ej. Alergia a la penicilina, asma leve..."
                  rows={3}
                  maxLength={150}
                  className={`${inputClass} resize-none`}
                />
                <p className="text-neutral-600 text-xs mt-1.5">
                  La persona escribe libremente. Rápido de construir, pero depende de que
                  sepa qué escribir y cómo.
                </p>
              </div>
            ) : (
              <div>
                <div className="flex flex-wrap gap-2 mb-2">
                  {CONDITION_CHIPS.map((chip) => {
                    const selected = selectedChips.includes(chip);
                    return (
                      <button
                        key={chip}
                        onClick={() => toggleChip(chip)}
                        className={`flex items-center gap-1.5 px-3 py-2 rounded-full text-sm font-semibold border transition-colors ${
                          selected
                            ? "bg-emerald-500 border-emerald-500 text-neutral-950"
                            : "bg-neutral-800 border-neutral-700 text-neutral-400 hover:border-neutral-600"
                        }`}
                      >
                        {chip === "Ninguna condición conocida" && <Tags size={12} />}
                        {chip}
                      </button>
                    );
                  })}
                </div>
                {!selectedChips.includes("Ninguna condición conocida") && (
                  <input
                    value={otherText}
                    onChange={(e) => setOtherText(e.target.value)}
                    placeholder="Otro (opcional)"
                    className={`${inputClass} mt-1`}
                  />
                )}
                <p className="text-neutral-600 text-xs mt-1.5">
                  Selección rápida y legible para el admin en una emergencia real, a cambio
                  de mantener esta lista fija.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Vista previa de lo que quedaría guardado, para facilitar la decisión */}
        <div className="mt-4 bg-neutral-900/60 border border-dashed border-neutral-800 rounded-2xl p-4">
          <p className="text-neutral-500 text-xs font-semibold uppercase tracking-wide mb-2">
            Esto es lo que se guardaría
          </p>
          <p className="text-neutral-300 text-sm">
            <span className="text-neutral-500">Contacto:</span> {contactName || "—"}{" "}
            {contactPhone && <span className="text-neutral-500">({contactPhone})</span>}
          </p>
          <p className="text-neutral-300 text-sm mt-1">
            <span className="text-neutral-500">Observaciones:</span> {observationsPreview}
          </p>
        </div>
      </div>
    </div>
  );
}

function Field({ label, icon, children }) {
  return (
    <div>
      <label className="text-neutral-300 text-sm font-semibold flex items-center gap-1.5 mb-1.5">
        {icon}
        {label}
      </label>
      {children}
    </div>
  );
}
