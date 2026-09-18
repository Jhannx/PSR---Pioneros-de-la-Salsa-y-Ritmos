import { useState } from "react";
import { Plus, Trash2, ChevronDown, ChevronUp, Phone, User, MessageSquareText } from "lucide-react";
import Field from "../forms/Field";
import MissingConsentPanel from "./MissingConsentPanel";
import { POLICY_EXCERPTS } from "../../constants/policyExcerpts";
import { isValidEmergencyContactPhone } from "../../utils/validation";

const MAX_CONTACTS = 5;

const defaultInputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-4 py-3 text-white text-base placeholder-neutral-600 outline-none focus:border-emerald-500";

// Sección opcional y plegable para las cuatro pantallas de alta (NOTAS-PARA-FRONTEND.md Bloque 2,
// C1) — antes esto solo se podía cargar estando logueado; ahora `PersonRequest` acepta
// `healthObservations`/`healthDataConsent`/`emergencyContacts` opcionales en las dos rutas de
// alta (`POST /api/personas` y `POST /api/personas/autoregistro`).
//
// Todo lo de acá vive en memoria hasta el envío final del formulario que lo contiene: a
// diferencia de MemberEmergencyContactPage.jsx (que guarda cada contacto con su propio POST al
// agregarlo), quien está registrándose todavía no tiene cuenta ni token — no hay a qué API
// llamar contacto por contacto. Por eso "agregar" acá solo mete la fila en `value.emergencyContacts`;
// el envío real ocurre una sola vez, junto con el resto del alta.
//
// `value`/`onChange` en un solo objeto — cada una de las cuatro pantallas solo necesita
// desestructurarlo tal cual sobre el body de `autoRegister`/`create`, sin armar nada.
//
// **Contrato de validación que debe respetar quien use este componente:** si
// `value.healthObservations` tiene contenido, `value.healthDataConsent` debe ser `true` antes de
// enviar — si no, el backend responde 400. Los contactos ya vienen validados: `addContact` (acá
// abajo) nunca empuja uno a la lista sin `thirdPartyConsent: true`, así que no hace falta volver
// a comprobarlo desde afuera.
export default function RegistrationHealthAndContacts({ value, onChange, inputClass = defaultInputClass }) {
  const { healthObservations, healthDataConsent, emergencyContacts } = value;

  const [open, setOpen] = useState(false);

  // Mini-formulario de "agregar contacto", separado de la lista ya confirmada — mismo patrón que
  // MemberEmergencyContactPage.jsx: el panel de autorización de terceros (política numeral 5)
  // solo aparece al intentar agregar, nunca mientras se está escribiendo nombre/teléfono, y se
  // pide de nuevo en cada contacto, sin excepción.
  const [addingContact, setAddingContact] = useState(false);
  const [newName, setNewName] = useState("");
  const [newPhone, setNewPhone] = useState("");
  const [newConsent, setNewConsent] = useState(false);
  const [consentPanelOpen, setConsentPanelOpen] = useState(false);

  const [healthConsentPanelOpen, setHealthConsentPanelOpen] = useState(false);

  const atLimit = emergencyContacts.length >= MAX_CONTACTS;
  const phoneValid = isValidEmergencyContactPhone(newPhone);

  function resetAddForm() {
    setAddingContact(false);
    setNewName("");
    setNewPhone("");
    setNewConsent(false);
    setConsentPanelOpen(false);
  }

  function handleAddContactClick() {
    if (!phoneValid) return;
    if (!consentPanelOpen) {
      setConsentPanelOpen(true);
      return;
    }
    if (!newConsent) return;
    onChange({
      ...value,
      emergencyContacts: [
        ...emergencyContacts,
        { name: newName.trim(), phone: newPhone.trim(), thirdPartyConsent: true },
      ],
    });
    resetAddForm();
  }

  function removeContact(index) {
    onChange({ ...value, emergencyContacts: emergencyContacts.filter((_, i) => i !== index) });
  }

  // Vaciar el campo no exige el checkbox — igual que en la pantalla de edición
  // (NOTAS-PARA-FRONTEND.md Bloque 1, §1.2): pedir permiso para dejar de tratar un dato sería al
  // revés. Si ya se había marcado el consentimiento y se vacía, se revoca junto con el texto.
  function handleHealthChange(text) {
    if (!text.trim()) {
      onChange({ ...value, healthObservations: text, healthDataConsent: false });
      setHealthConsentPanelOpen(false);
      return;
    }
    onChange({ ...value, healthObservations: text });
  }

  const healthConsentNeeded = healthObservations.trim().length > 0 && !healthDataConsent;

  return (
    <div className="border-t border-neutral-800 pt-4 mt-1">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="w-full flex items-center justify-between text-left text-neutral-300 text-sm font-semibold"
      >
        <span>Contacto de emergencia y salud (opcional)</span>
        {open ? (
          <ChevronUp size={16} className="text-neutral-500 shrink-0" />
        ) : (
          <ChevronDown size={16} className="text-neutral-500 shrink-0" />
        )}
      </button>
      <p className="text-neutral-600 text-xs mt-1">
        Puedes agregarlo ahora o más tarde desde tu perfil, cuando quieras.
      </p>

      {open && (
        <div className="mt-4 flex flex-col gap-4">
          {emergencyContacts.length > 0 && (
            <div className="flex flex-col gap-2">
              {emergencyContacts.map((contact, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between gap-3 bg-neutral-800/60 rounded-xl px-4 py-3"
                >
                  <div className="min-w-0">
                    <p className="text-white font-semibold text-sm truncate">{contact.name || "Sin nombre"}</p>
                    <p className="text-neutral-500 text-xs mt-0.5">{contact.phone}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => removeContact(index)}
                    title="Quitar contacto"
                    className="p-2 text-neutral-500 hover:text-rose-400 hover:bg-neutral-800 rounded-lg transition-colors shrink-0"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              ))}
            </div>
          )}

          {addingContact ? (
            <div className="flex flex-col gap-3">
              <Field label="Nombre del contacto" icon={<User size={14} />}>
                <input
                  autoFocus
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="Ej. Rosa Martínez (mamá)"
                  className={inputClass}
                />
              </Field>
              <Field label="Teléfono del contacto" icon={<Phone size={14} />}>
                <input
                  type="tel"
                  inputMode="tel"
                  value={newPhone}
                  onChange={(e) => setNewPhone(e.target.value)}
                  placeholder="300 987 6543"
                  className={inputClass}
                />
              </Field>
              {newPhone && !phoneValid && (
                <p className="text-amber-400 text-xs -mt-1.5">Ingresa un número de teléfono válido.</p>
              )}
              {consentPanelOpen && (
                <MissingConsentPanel
                  excerpt={POLICY_EXCERPTS.thirdParty}
                  context="agregar este contacto"
                  checked={newConsent}
                  onChange={setNewConsent}
                />
              )}
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={resetAddForm}
                  className="flex-1 border border-neutral-700 text-neutral-300 hover:bg-neutral-800 font-semibold py-2.5 rounded-xl transition-colors text-sm"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={handleAddContactClick}
                  disabled={!phoneValid || (consentPanelOpen && !newConsent)}
                  title={!phoneValid ? "Ingresa un número de teléfono válido" : undefined}
                  className="flex-1 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors text-sm"
                >
                  {consentPanelOpen ? "Confirmar y agregar" : "Agregar"}
                </button>
              </div>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setAddingContact(true)}
              disabled={atLimit}
              title={atLimit ? `Máximo ${MAX_CONTACTS} contactos` : undefined}
              className="flex items-center justify-center gap-1.5 border border-dashed border-neutral-700 text-neutral-400 hover:text-white hover:border-neutral-600 disabled:opacity-40 disabled:hover:text-neutral-400 disabled:hover:border-neutral-700 font-semibold py-2.5 rounded-xl transition-colors text-sm"
            >
              <Plus size={16} />
              {atLimit ? `Máximo ${MAX_CONTACTS} contactos` : "Agregar contacto de emergencia"}
            </button>
          )}

          <div className="border-t border-neutral-800 pt-4">
            <Field
              label="Observaciones de salud"
              icon={<MessageSquareText size={14} />}
              hint="Ej. Alergia a la penicilina, asma leve..."
            >
              <textarea
                value={healthObservations}
                onChange={(e) => handleHealthChange(e.target.value)}
                rows={3}
                maxLength={2000}
                className={`${inputClass} resize-none`}
              />
            </Field>

            {healthConsentNeeded &&
              (healthConsentPanelOpen ? (
                <div className="mt-3">
                  <MissingConsentPanel
                    excerpt={POLICY_EXCERPTS.health}
                    context="guardar tus observaciones de salud"
                    checked={healthDataConsent}
                    onChange={(checked) => onChange({ ...value, healthDataConsent: checked })}
                  />
                </div>
              ) : (
                <button
                  type="button"
                  onClick={() => setHealthConsentPanelOpen(true)}
                  className="mt-2 text-emerald-400 hover:text-emerald-300 text-xs font-semibold underline underline-offset-2"
                >
                  Autorizar el tratamiento de este dato →
                </button>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}
