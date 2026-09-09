import { useState } from "react";
import { Phone, User, MessageSquareText, Tags, CheckCircle2, ChevronLeft } from "lucide-react";
import Field from "../../components/forms/Field";

const CONDITION_CHIPS = [
  "Asma",
  "Diabetes",
  "Alergias",
  "Problemas cardíacos",
  "Epilepsia",
  "Ninguna condición conocida",
];

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-4 py-3 text-white text-base placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function MemberEmergencyContactPage({
  existingContact = null, // null = primer registro; si viene, se precarga para editar
  onBack,
}) {
  const [mode, setMode] = useState("chips"); // texto | chips — la persona elige, no es fijo

  const [contactName, setContactName] = useState(existingContact?.name ?? "");
  const [contactPhone, setContactPhone] = useState(existingContact?.phone ?? "");
  const [freeText, setFreeText] = useState(existingContact?.observations ?? "");
  const [selectedChips, setSelectedChips] = useState([]);
  const [otherText, setOtherText] = useState("");
  const [saved, setSaved] = useState(false);

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

  // Sin importar el modo elegido, siempre se guarda como un solo texto —
  // "observaciones" es una única columna, el modo solo cambia cómo se
  // construye ese texto, no dónde queda.
  const observationsToSave =
    mode === "texto"
      ? freeText.trim()
      : [...selectedChips, otherText.trim()].filter(Boolean).join(", ");

  const canSubmit = contactName.trim() || contactPhone.trim() || observationsToSave;

  function handleSubmit() {
    if (!canSubmit) return;
    // PUT /api/personas/{id}/contacto-emergencia
    // { name: contactName, phone: contactPhone, observations: observationsToSave }
    setSaved(true);
  }

  if (saved) {
    return (
      <div className="min-h-screen w-full bg-neutral-950 flex items-center justify-center px-4 py-10">
        <div className="w-full max-w-sm text-center">
          <div className="w-16 h-16 rounded-full bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-4">
            <CheckCircle2 className="text-emerald-400" size={32} />
          </div>
          <h1 className="text-xl font-bold text-white">Contacto de emergencia guardado</h1>
          <p className="text-neutral-400 text-sm mt-2">
            Puedes volver a editarlo cuando quieras desde tu perfil.
          </p>

          <div className="mt-6 bg-neutral-900 border border-neutral-800 rounded-2xl p-5 text-left">
            <p className="text-white font-semibold">{contactName || "—"}</p>
            {contactPhone && <p className="text-neutral-500 text-sm mt-0.5">{contactPhone}</p>}
            {observationsToSave && (
              <p className="text-neutral-400 text-sm mt-3 pt-3 border-t border-neutral-800">{observationsToSave}</p>
            )}
          </div>

          <button
            onClick={() => setSaved(false)}
            className="mt-6 text-sm text-neutral-500 hover:text-white underline underline-offset-2"
          >
            Editar de nuevo
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-start justify-center px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="mb-6">
          {onBack && (
            <button
              onClick={onBack}
              className="flex items-center gap-1 text-neutral-500 hover:text-white text-sm mb-4 transition-colors"
            >
              <ChevronLeft size={16} />
              Volver
            </button>
          )}
          <h1 className="text-2xl font-bold text-white">Contacto de emergencia</h1>
          <p className="text-neutral-400 text-sm mt-1">A quién llamar y qué debería saber quien responda.</p>
        </div>

        <div className="bg-neutral-900 rounded-3xl p-6 border border-neutral-800 flex flex-col gap-4">
          <div className="grid grid-cols-1 gap-4">
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
            <div className="flex items-center justify-between mb-3 flex-wrap gap-2">
              <label className="text-neutral-300 text-sm font-semibold flex items-center gap-1.5">
                <MessageSquareText size={14} />
                Observaciones
              </label>

              <div className="flex bg-neutral-800 rounded-lg p-1 text-xs font-semibold">
                <button
                  onClick={() => setMode("texto")}
                  className={`px-3 py-1.5 rounded-md transition-colors ${
                    mode === "texto" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400"
                  }`}
                >
                  Escribir
                </button>
                <button
                  onClick={() => setMode("chips")}
                  className={`px-3 py-1.5 rounded-md transition-colors ${
                    mode === "chips" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400"
                  }`}
                >
                  Elegir de una lista
                </button>
              </div>
            </div>

            {mode === "texto" ? (
              <textarea
                value={freeText}
                onChange={(e) => setFreeText(e.target.value)}
                placeholder="Ej. Alergia a la penicilina, asma leve..."
                rows={3}
                maxLength={150}
                className={`${inputClass} resize-none`}
              />
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
              </div>
            )}
          </div>
        </div>

        <button
          onClick={handleSubmit}
          disabled={!canSubmit}
          className="w-full mt-6 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 active:scale-[0.98] transition-all text-neutral-950 font-bold text-lg py-5 rounded-2xl shadow-lg shadow-emerald-500/20"
        >
          Guardar
        </button>
      </div>
    </div>
  );
}
