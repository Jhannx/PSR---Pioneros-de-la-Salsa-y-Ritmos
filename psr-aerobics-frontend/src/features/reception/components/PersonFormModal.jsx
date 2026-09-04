import { useState } from "react";
import { X } from "lucide-react";

export default function PersonFormModal({ title, initialData, onSave, onClose }) {
  const [firstName, setFirstName] = useState(initialData?.firstName || "");
  const [lastName, setLastName] = useState(initialData?.lastName || "");
  const [documento, setDocumento] = useState(initialData?.documento || "");
  const [phone, setPhone] = useState(initialData?.phone || "");

  const canSave = firstName.trim() && lastName.trim() && documento.trim() && phone.trim();

  function handleSave() {
    if (!canSave) return;
    onSave({
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      documento: documento.trim(),
      phone: phone.trim(),
    });
  }

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50">
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 w-full max-w-md">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-white font-bold text-lg">{title}</h2>
          <button onClick={onClose} className="text-neutral-500 hover:text-white">
            <X size={20} />
          </button>
        </div>

        <div className="grid grid-cols-2 gap-3 mb-3">
          <div>
            <label className="text-neutral-400 text-xs font-medium">Nombre</label>
            <input
              autoFocus
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              placeholder="Rosa"
              className="w-full mt-1 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white text-sm placeholder-neutral-600 outline-none focus:border-emerald-500"
            />
          </div>
          <div>
            <label className="text-neutral-400 text-xs font-medium">Apellidos</label>
            <input
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              placeholder="Martínez López"
              className="w-full mt-1 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white text-sm placeholder-neutral-600 outline-none focus:border-emerald-500"
            />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 mb-5">
          <div>
            <label className="text-neutral-400 text-xs font-medium">Documento</label>
            <input
              inputMode="numeric"
              value={documento}
              onChange={(e) => setDocumento(e.target.value.replace(/\D/g, ""))}
              placeholder="1001234567"
              className="w-full mt-1 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white text-sm placeholder-neutral-600 outline-none focus:border-emerald-500"
            />
          </div>
          <div>
            <label className="text-neutral-400 text-xs font-medium">Teléfono</label>
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="300 123 4567"
              className="w-full mt-1 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white text-sm placeholder-neutral-600 outline-none focus:border-emerald-500"
            />
          </div>
        </div>

        {!initialData && (
          <p className="text-neutral-600 text-xs mb-4 -mt-2">
            Se crea con contraseña por defecto = su documento. La persona puede cambiarla luego desde su perfil.
          </p>
        )}

        <button
          onClick={handleSave}
          disabled={!canSave}
          className="w-full bg-emerald-500 hover:bg-emerald-400 disabled:opacity-40 disabled:hover:bg-emerald-500 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
        >
          Guardar
        </button>
      </div>
    </div>
  );
}
