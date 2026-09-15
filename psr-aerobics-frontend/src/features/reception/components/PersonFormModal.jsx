import { useState } from "react";
import { X } from "lucide-react";
import BirthDateField from "../../../components/forms/BirthDateField";
import { isValidDocumentNumber, isValidPhone } from "../../../utils/validation";

export default function PersonFormModal({ mode = "add", person, onSave, onClose, error, saving = false }) {
  const [firstName, setFirstName] = useState(person?.firstName || "");
  const [lastName, setLastName] = useState(person?.lastName || "");
  const [documentType, setDocumentType] = useState(person?.documentType || "CC");
  const [documentNumber, setDocumentNumber] = useState(person?.documentNumber || person?.documento || "");
  const [phone, setPhone] = useState(person?.phone || "");
  const [email, setEmail] = useState(person?.email || "");
  const [birthDate, setBirthDate] = useState(person?.birthDate || "");

  // Front-only, ver docs/errores-13sep-front.md #3 — no reemplaza la validación real del backend.
  // El teléfono sigue siendo opcional en esta pantalla (igual que antes), pero si se escribe algo
  // debe parecer un teléfono real, no solo "no estar vacío".
  const documentoValid = isValidDocumentNumber(documentNumber);
  const phoneValid = !phone.trim() || isValidPhone(phone);

  // Obligatorio para registros nuevos, no para editar uno existente (NOTAS-PARA-FRONTEND.md §5):
  // las personas registradas antes de este campo no lo tienen, y no hay por qué exigírselo recién
  // al editar cualquier otro dato suyo.
  const canSave =
    firstName.trim() &&
    lastName.trim() &&
    documentoValid &&
    phoneValid &&
    (mode === "edit" || birthDate);

  function handleSave() {
    if (!canSave) return;
    onSave({
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      documentType,
      documentNumber: documentNumber.trim(),
      phone: phone.trim(),
      email: email.trim() || null,
      birthDate: birthDate || null,
    });
  }

  const title = mode === "edit" ? "Editar Persona" : "Agregar Persona";

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50 font-body">
      <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-white font-bold text-lg font-display">{title}</h2>
          <button onClick={onClose} className="text-neutral-500 hover:text-white">
            <X size={20} />
          </button>
        </div>

        <div className="space-y-3 mb-5">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-neutral-400 text-xs font-semibold">Nombres</label>
              <input
                autoFocus
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                placeholder="Rosa"
                className="w-full mt-1 bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-white text-sm placeholder-neutral-600 outline-none focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="text-neutral-400 text-xs font-semibold">Apellidos</label>
              <input
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                placeholder="Martínez López"
                className="w-full mt-1 bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-white text-sm placeholder-neutral-600 outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-2">
            <div>
              <label className="text-neutral-400 text-xs font-semibold">Tipo Doc.</label>
              <select
                value={documentType}
                onChange={(e) => setDocumentType(e.target.value)}
                className="w-full mt-1 bg-neutral-950 border border-neutral-800 rounded-xl px-2.5 py-2.5 text-white text-sm outline-none focus:border-emerald-500"
              >
                <option value="CC">CC</option>
                <option value="TI">TI</option>
                <option value="CE">CE</option>
                <option value="PA">PA</option>
              </select>
            </div>
            <div className="col-span-2">
              <label className="text-neutral-400 text-xs font-semibold">No. Documento</label>
              <input
                inputMode="numeric"
                value={documentNumber}
                onChange={(e) => setDocumentNumber(e.target.value.replace(/\D/g, ""))}
                placeholder="1001234567"
                className="w-full mt-1 bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-white text-sm placeholder-neutral-600 outline-none focus:border-emerald-500"
              />
              {documentNumber && !documentoValid && (
                <p className="text-amber-400 text-[11px] mt-1">Mínimo 6 dígitos.</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-neutral-400 text-xs font-semibold">Teléfono</label>
              <input
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="300 123 4567"
                className="w-full mt-1 bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-white text-sm placeholder-neutral-600 outline-none focus:border-emerald-500"
              />
              {phone && !phoneValid && <p className="text-amber-400 text-[11px] mt-1">Mínimo 10 dígitos.</p>}
            </div>
            <div>
              <label className="text-neutral-400 text-xs font-semibold">Correo Electrónico (opcional)</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="correo@ejemplo.com"
                className="w-full mt-1 bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-white text-sm placeholder-neutral-600 outline-none focus:border-emerald-500"
              />
            </div>
          </div>

          <div>
            <label className="text-neutral-400 text-xs font-semibold">
              Fecha de nacimiento{mode === "edit" ? " (opcional)" : ""}
            </label>
            <BirthDateField
              value={birthDate}
              onChange={setBirthDate}
              className="mt-1"
              selectClassName="bg-neutral-950 border border-neutral-800 rounded-xl px-2 py-2.5 text-white text-sm outline-none focus:border-emerald-500"
            />
            {mode === "edit" && !person?.birthDate && (
              <p className="text-neutral-600 text-[11px] mt-1">
                Esta persona no tiene fecha de nacimiento registrada todavía.
              </p>
            )}
          </div>
        </div>

        {mode === "add" && (
          <p className="text-neutral-500 text-xs mb-4">
            Se crea con contraseña por defecto igual a su número de documento. Los roles se asignan luego desde "Roles y Accesos".
          </p>
        )}

        {/* El error de guardar vivía solo en la página de atrás, que este modal tapa por
            completo (z-50 + fondo oscurecido) — un 409 de documento/teléfono duplicado no se
            veía nunca, aunque el estado sí se actualizara (errores-12sep-front.md #9). */}
        {error && (
          <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl">
            {error}
          </div>
        )}

        <div className="flex justify-end gap-2 pt-2">
          <button
            onClick={onClose}
            disabled={saving}
            className="px-4 py-2.5 text-neutral-400 hover:text-white text-sm font-semibold disabled:opacity-40"
          >
            Cancelar
          </button>
          <button
            onClick={handleSave}
            disabled={!canSave || saving}
            className="bg-emerald-500 hover:bg-emerald-400 disabled:opacity-40 text-neutral-950 font-bold text-sm px-5 py-2.5 rounded-xl transition-colors shadow-lg shadow-emerald-500/20"
          >
            {saving ? "Guardando…" : "Guardar Persona"}
          </button>
        </div>
      </div>
    </div>
  );
}
