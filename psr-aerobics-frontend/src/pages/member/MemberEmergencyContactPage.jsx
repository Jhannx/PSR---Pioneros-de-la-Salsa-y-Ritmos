import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Phone, User, MessageSquareText, Tags, CheckCircle2, ChevronLeft, Plus, Trash2 } from "lucide-react";
import Field from "../../components/forms/Field";
import { useAuth } from "../../context/AuthContext";
import { peopleService } from "../../services/peopleService";

const CONDITION_CHIPS = [
  "Asma",
  "Diabetes",
  "Alergias",
  "Problemas cardíacos",
  "Epilepsia",
  "Ninguna condición conocida",
];

const MAX_CONTACTS = 5;

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-4 py-3 text-white text-base placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

// Rediseño completo, no un ajuste — antes era un formulario de un solo contacto con
// observaciones adentro; ahora es una lista de hasta 5 contactos, todos con el mismo peso (sin
// "principal"), más un campo de observaciones de salud aparte, porque ya no le pertenecen a
// ningún contacto puntual (NOTAS-PARA-FRONTEND.md §6, CAMBIO ROMPEDOR).
//
// Degradación elegante (docs/PLAN-IMPLEMENTACION.md §5.6): estos endpoints están escritos en el
// backend pero todavía no verificados contra una base real (falta F6 verde). Si responden
// 404/500, esta pantalla muestra un estado vacío o un mensaje legible en vez de romperse.
export default function MemberEmergencyContactPage({ onBack }) {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [contacts, setContacts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  // Formulario para agregar un contacto nuevo — separado de la lista, no hay "editar en línea"
  // en esta primera versión: agregar/quitar cubre el caso de uso real (corregir un dato mal
  // escrito es quitar y volver a agregar).
  const [addingContact, setAddingContact] = useState(false);
  const [newName, setNewName] = useState("");
  const [newPhone, setNewPhone] = useState("");
  const [savingContact, setSavingContact] = useState(false);
  const [addError, setAddError] = useState("");
  const [removingId, setRemovingId] = useState(null);
  const [removeError, setRemoveError] = useState("");

  // Observaciones de salud — de la persona, no de un contacto. Modo texto/chips igual que antes,
  // pero lo que se guarda ya no depende de si hay contactos o no.
  const [healthMode, setHealthMode] = useState("chips"); // texto | chips — la persona elige
  const [freeText, setFreeText] = useState("");
  const [selectedChips, setSelectedChips] = useState([]);
  const [otherText, setOtherText] = useState("");
  const [savedHealthValue, setSavedHealthValue] = useState(""); // último valor confirmado guardado
  const [savingHealth, setSavingHealth] = useState(false);
  const [healthError, setHealthError] = useState("");
  const [healthSaved, setHealthSaved] = useState(false);

  useEffect(() => {
    if (!user?.personId) {
      // Diferido a una microtarea — llamar setState de forma sincrónica dentro del cuerpo de un
      // efecto dispara el lint de "cascading renders" (mismo patrón que ya usaba esta pantalla).
      Promise.resolve().then(() => {
        setLoadError("Esta cuenta no está asociada a una persona.");
        setLoading(false);
      });
      return;
    }
    let cancelled = false;
    async function load() {
      try {
        const [contactsResult, profile] = await Promise.allSettled([
          peopleService.getEmergencyContacts(user.personId),
          peopleService.getProfile(user.personId),
        ]);
        if (cancelled) return;

        if (contactsResult.status === "fulfilled") {
          setContacts(Array.isArray(contactsResult.value) ? contactsResult.value : []);
        } else if (contactsResult.reason?.status !== 404) {
          // 404 es un caso legítimo (todavía sin contactos) — cualquier otro error sí se avisa.
          setLoadError(
            contactsResult.reason?.data?.message || contactsResult.reason?.message || "No fue posible cargar tus contactos."
          );
        }

        if (profile.status === "fulfilled") {
          const value = profile.value?.healthObservations || "";
          setSavedHealthValue(value);
          setFreeText(value);
        } else {
          // Antes esto fallaba en silencio (errores-12sep-front.md #2): un 500 de
          // GET /api/personas/{id}/perfil dejaba el campo de observaciones vacío sin avisar,
          // indistinguible de "todavía no tiene nada guardado". `prev || ...` para no pisar un
          // error de contactos que ya se haya mostrado arriba.
          setLoadError(
            (prev) =>
              prev || profile.reason?.data?.message || profile.reason?.message || "No fue posible cargar tu perfil."
          );
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [user?.personId]);

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

  const healthValueToSave =
    healthMode === "texto"
      ? freeText.trim()
      : [...selectedChips, otherText.trim()].filter(Boolean).join(", ");

  // El backend solo exige que *algún* campo no sea nulo (`chk_contacto_emergencia_no_vacio`), lo
  // que hoy deja crear un contacto sin ningún teléfono válido — inútil en una emergencia real, no
  // hay a quién llamar (errores-12sep-compartido.md #2). El frontend exige acá un teléfono con
  // formato razonable antes de dejar guardar, sin esperar a que el backend lo valide algún día.
  const phoneDigits = newPhone.replace(/\D/g, "");
  const phoneTooShort = newPhone.trim().length > 0 && phoneDigits.length < 7;
  const phoneValid = phoneDigits.length >= 7 && phoneDigits.length <= 15;

  async function handleAddContact() {
    const name = newName.trim();
    const phone = newPhone.trim();
    if (!phoneValid) return;
    setSavingContact(true);
    setAddError("");
    try {
      const created = await peopleService.createEmergencyContact(user.personId, { name, phone });
      setContacts((prev) => [...prev, created]);
      setNewName("");
      setNewPhone("");
      setAddingContact(false);
    } catch (err) {
      // 409 = ya hay 5 — no debería llegarse acá porque el botón se deshabilita antes, pero se
      // captura igual por si dos pestañas agregan a la vez.
      setAddError(err.data?.message || err.message || "No fue posible agregar el contacto.");
    } finally {
      setSavingContact(false);
    }
  }

  async function handleRemoveContact(contactId) {
    setRemovingId(contactId);
    setRemoveError("");
    try {
      await peopleService.deleteEmergencyContact(contactId);
      setContacts((prev) => prev.filter((c) => c.id !== contactId));
    } catch (err) {
      setRemoveError(err.data?.message || err.message || "No fue posible quitar el contacto.");
    } finally {
      setRemovingId(null);
    }
  }

  async function handleSaveHealth() {
    setSavingHealth(true);
    setHealthError("");
    setHealthSaved(false);
    try {
      await peopleService.updateHealthObservations(user.personId, healthValueToSave || null);
      setSavedHealthValue(healthValueToSave);
      setHealthSaved(true);
      setTimeout(() => setHealthSaved(false), 3000);
    } catch (err) {
      setHealthError(err.data?.message || err.message || "No fue posible guardar las observaciones.");
    } finally {
      setSavingHealth(false);
    }
  }

  const atLimit = contacts.length >= MAX_CONTACTS;
  const healthDirty = healthValueToSave !== savedHealthValue;

  if (loading) return <div className="min-h-screen bg-neutral-950 text-neutral-400 p-8">Cargando…</div>;

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

        {loadError && (
          <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm">
            {loadError}
          </div>
        )}

        {/* Lista de contactos — todos con el mismo peso, sin orden de prioridad. */}
        <div className="bg-neutral-900 rounded-3xl p-6 border border-neutral-800 flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <h2 className="text-white font-bold text-sm flex items-center gap-1.5">
              <User size={14} />
              Contactos ({contacts.length}/{MAX_CONTACTS})
            </h2>
          </div>

          {removeError && <p className="text-rose-400 text-xs">{removeError}</p>}

          {contacts.length === 0 && !addingContact ? (
            <p className="text-neutral-500 text-sm">Todavía no tienes contactos de emergencia registrados.</p>
          ) : (
            <div className="flex flex-col gap-2">
              {contacts.map((contact) => (
                <div
                  key={contact.id}
                  className="flex items-center justify-between gap-3 bg-neutral-800/60 rounded-xl px-4 py-3"
                >
                  <div className="min-w-0">
                    <p className="text-white font-semibold text-sm truncate">{contact.name || "Sin nombre"}</p>
                    {contact.phone && <p className="text-neutral-500 text-xs mt-0.5">{contact.phone}</p>}
                  </div>
                  <button
                    onClick={() => handleRemoveContact(contact.id)}
                    disabled={removingId === contact.id}
                    title="Quitar contacto"
                    className="p-2 text-neutral-500 hover:text-rose-400 hover:bg-neutral-800 rounded-lg transition-colors shrink-0 disabled:opacity-40"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              ))}
            </div>
          )}

          {addingContact ? (
            <div className="border-t border-neutral-800 pt-4 mt-1 flex flex-col gap-3">
              <Field label="Nombre" icon={<User size={14} />}>
                <input
                  autoFocus
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="Ej. Rosa Martínez (mamá)"
                  className={inputClass}
                />
              </Field>
              <Field label="Teléfono" icon={<Phone size={14} />}>
                <input
                  type="tel"
                  inputMode="tel"
                  value={newPhone}
                  onChange={(e) => setNewPhone(e.target.value)}
                  placeholder="300 987 6543"
                  className={inputClass}
                />
              </Field>
              {phoneTooShort && (
                <p className="text-amber-400 text-xs -mt-1.5">Ingresa un número de teléfono válido.</p>
              )}
              {addError && <p className="text-rose-400 text-xs">{addError}</p>}
              <div className="flex gap-2">
                <button
                  onClick={() => {
                    setAddingContact(false);
                    setAddError("");
                    setNewName("");
                    setNewPhone("");
                  }}
                  className="flex-1 border border-neutral-700 text-neutral-300 hover:bg-neutral-800 font-semibold py-2.5 rounded-xl transition-colors text-sm"
                >
                  Cancelar
                </button>
                <button
                  onClick={handleAddContact}
                  disabled={!phoneValid || savingContact}
                  title={!phoneValid ? "Ingresa un número de teléfono válido" : undefined}
                  className="flex-1 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors text-sm"
                >
                  {savingContact ? "Agregando…" : "Agregar"}
                </button>
              </div>
            </div>
          ) : (
            <button
              onClick={() => setAddingContact(true)}
              disabled={atLimit}
              title={atLimit ? `Máximo ${MAX_CONTACTS} contactos` : undefined}
              className="mt-1 flex items-center justify-center gap-1.5 border border-dashed border-neutral-700 text-neutral-400 hover:text-white hover:border-neutral-600 disabled:opacity-40 disabled:hover:text-neutral-400 disabled:hover:border-neutral-700 font-semibold py-2.5 rounded-xl transition-colors text-sm"
            >
              <Plus size={16} />
              {atLimit ? `Máximo ${MAX_CONTACTS} contactos` : "Agregar contacto"}
            </button>
          )}
        </div>

        {/* Observaciones de salud — de la persona, ya no de un contacto puntual. */}
        <div className="bg-neutral-900 rounded-3xl p-6 border border-neutral-800 flex flex-col gap-3 mt-4">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <label className="text-neutral-300 text-sm font-semibold flex items-center gap-1.5">
              <MessageSquareText size={14} />
              Observaciones de salud
            </label>
            <div className="flex bg-neutral-800 rounded-lg p-1 text-xs font-semibold">
              <button
                onClick={() => setHealthMode("texto")}
                className={`px-3 py-1.5 rounded-md transition-colors ${
                  healthMode === "texto" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400"
                }`}
              >
                Escribir
              </button>
              <button
                onClick={() => setHealthMode("chips")}
                className={`px-3 py-1.5 rounded-md transition-colors ${
                  healthMode === "chips" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400"
                }`}
              >
                Elegir de una lista
              </button>
            </div>
          </div>

          {healthMode === "texto" ? (
            <textarea
              value={freeText}
              onChange={(e) => setFreeText(e.target.value)}
              placeholder="Ej. Alergia a la penicilina, asma leve..."
              rows={3}
              maxLength={2000}
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

          {healthError && <p className="text-rose-400 text-xs">{healthError}</p>}
          {healthSaved && (
            <p className="text-emerald-400 text-xs flex items-center gap-1.5">
              <CheckCircle2 size={13} /> Observaciones guardadas.
            </p>
          )}

          <button
            onClick={handleSaveHealth}
            disabled={!healthDirty || savingHealth}
            className="w-full bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 active:scale-[0.98] transition-all text-neutral-950 font-bold py-3 rounded-xl"
          >
            {savingHealth ? "Guardando…" : healthDirty ? "Guardar observaciones" : "Guardado"}
          </button>
        </div>

        <button
          onClick={() => navigate("/profile")}
          className="mt-6 mx-auto flex items-center gap-1.5 text-sm text-neutral-500 hover:text-white underline underline-offset-2"
        >
          Volver a mi perfil
        </button>
      </div>
    </div>
  );
}
