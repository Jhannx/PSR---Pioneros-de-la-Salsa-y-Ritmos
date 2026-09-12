import { useState } from "react";
import { X } from "lucide-react";
import BirthDateField from "../../../components/forms/BirthDateField";

export default function PersonFormModal({ mode = "add", person, onSave, onClose }) {
  const [firstName, setFirstName] = useState(person?.firstName || "");
  const [lastName, setLastName] = useState(person?.lastName || "");
  const [documentType, setDocumentType] = useState(person?.documentType || "CC");
  const [documentNumber, setDocumentNumber] = useState(person?.documentNumber || person?.documento || "");
  const [phone, setPhone] = useState(person?.phone || "");
  const [email, setEmail] = useState(person?.email || "");
  const [birthDate, setBirthDate] = useState(person?.birthDate || "");

  // Obligatorio para registros nuevos, no para editar uno existente (NOTAS-PARA-FRONTEND.md §5):
  // las personas registradas antes de este campo no lo tienen, y no hay por qué exigírselo recién
  // al editar cualquier otro dato suyo.
  const canSave =
    firstName.trim() && lastName.trim() && documentNumber.trim() && (mode === "edit" || birthDate);

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

        <div className="flex justify-end gap-2 pt-2">
          <button
            onClick={onClose}
            className="px-4 py-2.5 text-neutral-400 hover:text-white text-sm font-semibold"
          >
            Cancelar
          </button>
          <button
            onClick={handleSave}
            disabled={!canSave}
            className="bg-emerald-500 hover:bg-emerald-400 disabled:opacity-40 text-neutral-950 font-bold text-sm px-5 py-2.5 rounded-xl transition-colors shadow-lg shadow-emerald-500/20"
          >
            Guardar Persona
          </button>
        </div>
      </div>
    </div>
  );
}
