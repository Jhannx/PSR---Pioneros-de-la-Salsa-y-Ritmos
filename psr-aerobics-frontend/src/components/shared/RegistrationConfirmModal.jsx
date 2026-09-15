import { ShieldCheck } from "lucide-react";
import { formatBirthDate } from "../../utils/date";

// Panel de confirmación antes del envío final del autoregistro (mejoras-frontend.md §1) —
// mismo patrón visual que los demás modales de confirmación del proyecto (ej. HardDeleteModal):
// fondo oscurecido, tarjeta centrada, volver o confirmar. Alcance confirmado: solo
// SelfRegistrationPage y LobbyRegistrationPage. No aplica a PersonFormModal (el alta que hace
// recepción/admin) — ahí ya hay una persona supervisando el dato antes de guardarlo, el riesgo
// que motiva esto (un typo que nadie más revisa) no existe igual.
export default function RegistrationConfirmModal({ data, onConfirm, onClose, submitting = false }) {
  const rows = [
    { label: "Nombre completo", value: `${data.firstName} ${data.lastName}`.trim() },
    { label: "Documento", value: `${data.documentType} ${data.documento}` },
    { label: "Teléfono", value: data.phone },
    { label: "Correo electrónico", value: data.email || "— (opcional, no diste uno)" },
    { label: "Fecha de nacimiento", value: formatBirthDate(data.birthDate) || "—" },
  ];

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50 font-body">
      <div className="bg-neutral-900 border-2 border-emerald-500/40 rounded-2xl p-6 w-full max-w-sm">
        <div className="flex items-center gap-2 mb-3">
          <ShieldCheck className="text-emerald-400" size={20} />
          <h2 className="text-white font-bold text-lg">Confirma tus datos</h2>
        </div>
        <p className="text-neutral-400 text-sm mb-4">
          Revisa que todo esté bien escrito antes de crear tu perfil — en particular el documento y el
          teléfono, que son los que usamos para identificarte.
        </p>

        <div className="bg-neutral-950 border border-neutral-800 rounded-xl divide-y divide-neutral-800/80 mb-5">
          {rows.map((row) => (
            <div key={row.label} className="px-4 py-2.5 flex items-center justify-between gap-3">
              <span className="text-neutral-500 text-xs shrink-0">{row.label}</span>
              <span className="text-white text-sm font-semibold text-right truncate">{row.value}</span>
            </div>
          ))}
        </div>

        <div className="flex gap-2">
          <button
            onClick={onClose}
            disabled={submitting}
            className="flex-1 border border-neutral-700 text-neutral-300 hover:bg-neutral-800 disabled:opacity-40 font-semibold py-2.5 rounded-xl transition-colors"
          >
            Volver a editar
          </button>
          <button
            onClick={onConfirm}
            disabled={submitting}
            className="flex-1 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
          >
            {submitting ? "Creando..." : "Confirmar y crear"}
          </button>
        </div>
      </div>
    </div>
  );
}
