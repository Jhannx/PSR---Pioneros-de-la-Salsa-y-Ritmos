import { Pencil, Trash2, CreditCard, ShieldAlert } from "lucide-react";
import { getInitialsFromParts as getInitials } from "../../../utils/text";

export default function PersonRow({
  person,
  isConfirmingDeactivate,
  onEdit,
  onDeactivateClick,
  onConfirmDeactivateYes,
  onConfirmDeactivateCancel,
  onHardDeleteClick,
}) {
  return (
    <div className="flex items-center gap-4 px-4 py-3 rounded-xl border border-neutral-800 bg-neutral-900 transition-colors">
      <div className="w-10 h-10 rounded-full bg-neutral-800 flex items-center justify-center text-sm font-bold text-neutral-300 shrink-0">
        {getInitials(person.firstName, person.lastName)}
      </div>

      <div className="flex-1 min-w-0">
        <p className="text-white font-semibold truncate">
          {person.firstName} {person.lastName}
        </p>
        <p className="text-xs text-neutral-500">
          {person.phone}
          <span className="text-neutral-600"> · {person.source === "qr" ? "autoregistro" : "manual"}</span>
        </p>
      </div>

      <div className="hidden sm:flex items-center gap-1.5 text-neutral-400 bg-neutral-800 px-2.5 py-1 rounded-lg text-xs font-semibold shrink-0">
        <CreditCard size={12} />
        {person.documento}
      </div>

      {isConfirmingDeactivate ? (
        <div className="flex items-center gap-2 shrink-0">
          <span className="text-xs text-neutral-400">¿Desactivar?</span>
          <button onClick={onConfirmDeactivateYes} className="text-xs font-semibold px-3 py-1.5 rounded-lg bg-amber-500 text-neutral-950 hover:bg-amber-400 transition-colors">
            Sí
          </button>
          <button onClick={onConfirmDeactivateCancel} className="text-xs font-semibold px-3 py-1.5 rounded-lg border border-neutral-700 text-neutral-400 hover:bg-neutral-800 transition-colors">
            Cancelar
          </button>
        </div>
      ) : (
        <div className="flex items-center gap-1 shrink-0">
          <button onClick={onEdit} title="Editar" className="text-neutral-500 hover:text-white p-2 rounded-lg hover:bg-neutral-800 transition-colors">
            <Pencil size={16} />
          </button>
          <button onClick={onDeactivateClick} title="Desactivar (dejó la academia)" className="text-neutral-500 hover:text-amber-400 p-2 rounded-lg hover:bg-neutral-800 transition-colors">
            <Trash2 size={16} />
          </button>
          <button onClick={onHardDeleteClick} title="Eliminar definitivamente" className="text-neutral-600 hover:text-red-400 p-2 rounded-lg hover:bg-neutral-800 transition-colors">
            <ShieldAlert size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
