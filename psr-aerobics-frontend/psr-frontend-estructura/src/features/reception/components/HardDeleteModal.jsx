import { useState } from "react";
import { ShieldAlert } from "lucide-react";

// RF-08 — eliminación definitiva, irreversible. Requiere escribir el
// nombre completo para habilitar el botón — a propósito no es un click
// único, esto no se puede deshacer.
export default function HardDeleteModal({ person, onConfirm, onClose }) {
  const [confirmText, setConfirmText] = useState("");
  const fullName = `${person.firstName} ${person.lastName}`;
  const canConfirm = confirmText.trim().toLowerCase() === fullName.trim().toLowerCase();

  return (
    <div className="fixed inset-0 bg-black/80 flex items-center justify-center px-4 z-50">
      <div className="bg-neutral-900 border-2 border-red-500/40 rounded-2xl p-6 w-full max-w-md">
        <div className="flex items-center gap-2 mb-3">
          <ShieldAlert className="text-red-400" size={20} />
          <h2 className="text-white font-bold text-lg">Eliminar definitivamente</h2>
        </div>
        <p className="text-neutral-400 text-sm mb-4">
          Esto borra el nombre, documento y teléfono de{" "}
          <span className="text-white font-semibold">{fullName}</span> de forma permanente — no se puede deshacer.
          Su historial de asistencias y pagos se conserva, pero deja de estar ligado a esta persona: aparecerá como
          "Persona eliminada".
        </p>
        <label className="text-neutral-400 text-xs font-medium">
          Escribe "{fullName}" para confirmar
        </label>
        <input
          autoFocus
          value={confirmText}
          onChange={(e) => setConfirmText(e.target.value)}
          className="w-full mt-1 mb-4 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white text-sm outline-none focus:border-red-500"
        />
        <div className="flex gap-2">
          <button
            onClick={onClose}
            className="flex-1 border border-neutral-700 text-neutral-300 hover:bg-neutral-800 font-semibold py-2.5 rounded-xl transition-colors"
          >
            Cancelar
          </button>
          <button
            onClick={onConfirm}
            disabled={!canConfirm}
            className="flex-1 bg-red-500 disabled:opacity-30 disabled:hover:bg-red-500 hover:bg-red-400 text-white font-bold py-2.5 rounded-xl transition-colors"
          >
            Eliminar para siempre
          </button>
        </div>
      </div>
    </div>
  );
}
