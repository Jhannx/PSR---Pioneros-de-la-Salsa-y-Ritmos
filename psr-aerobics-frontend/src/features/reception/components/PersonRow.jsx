import { Edit2, UserX, UserCheck2, Trash2, Check, X } from "lucide-react";

const ROLE_LABELS = { ADMIN: "Admin", RECEPTIONIST: "Recepción", TEACHER: "Profesor", MEMBER: "Miembro" };

export default function PersonRow({
  person,
  isDeactivating,
  onEdit,
  onDeactivateRequest,
  onDeactivateConfirm,
  onDeactivateCancel,
  onReactivate,
  onHardDeleteRequest,
  readOnly = false,
  canHardDelete = false,
}) {
  const fullName = `${person.firstName} ${person.lastName}`.trim();
  const sourceLabel = person.source === "SELF_REGISTRATION" ? "Autoregistro" : "Manual";
  const roleLabels = (person.roles || []).map((r) => ROLE_LABELS[r] || r);
  const isInactive = person.active === false;

  return (
    <div
      className={`flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-4 transition-colors border-b border-neutral-800/60 font-body ${
        isInactive ? "opacity-60" : "hover:bg-neutral-900/40"
      }`}
    >
      <div className="flex items-center gap-3 min-w-0 flex-1">
        <div className="w-10 h-10 rounded-full bg-neutral-800 text-emerald-400 font-bold flex items-center justify-center text-sm border border-neutral-700 shrink-0">
          {person.firstName ? person.firstName[0] : "P"}
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-white font-bold text-base truncate flex items-center gap-2" title={fullName}>
            {fullName}
            {isInactive && (
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-neutral-800 border border-neutral-700 text-neutral-400">
                Inactivo
              </span>
            )}
          </p>
          <div className="flex flex-wrap items-center gap-2 text-xs text-neutral-400 mt-0.5">
            <span className="font-mono">Doc: {person.documentType} {person.documentNumber}</span>
            <span>·</span>
            <span>Tel: {person.phone || "Sin teléfono"}</span>
            {person.email && (
              <>
                <span>·</span>
                <span className="truncate max-w-[180px]">{person.email}</span>
              </>
            )}
          </div>
          {roleLabels.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-1.5">
              {roleLabels.map((label) => (
                <span
                  key={label}
                  className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-purple-500/10 border border-purple-500/30 text-purple-300"
                >
                  {label}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Pastilla redondeada de fuente de registro (rounded-full) */}
      <div className="flex items-center gap-3 shrink-0">
        <span className="text-xs font-semibold px-3 py-1 rounded-full bg-neutral-800 border border-neutral-700 text-neutral-300">
          {sourceLabel}
        </span>

        {/* Acciones de fila */}
        {readOnly ? null : isDeactivating ? (
          <div className="flex items-center gap-1 bg-red-500/10 border border-red-500/30 p-1 rounded-full">
            <span className="text-xs font-bold text-red-400 px-2">¿Desactivar?</span>
            <button
              onClick={onDeactivateConfirm}
              className="p-1 text-emerald-400 hover:bg-emerald-500/20 rounded-full"
              title="Confirmar desactivación"
            >
              <Check size={14} />
            </button>
            <button
              onClick={onDeactivateCancel}
              className="p-1 text-neutral-400 hover:bg-neutral-800 rounded-full"
              title="Cancelar"
            >
              <X size={14} />
            </button>
          </div>
        ) : isInactive ? (
          <div className="flex items-center gap-1">
            <button
              onClick={onReactivate}
              title="Reactivar persona"
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold bg-emerald-500/15 text-emerald-400 hover:bg-emerald-500/25 transition-colors"
            >
              <UserCheck2 size={14} /> Reactivar
            </button>
            {canHardDelete && (
              <button
                onClick={onHardDeleteRequest}
                title="Eliminar definitivamente (hard-delete)"
                className="p-2 text-neutral-400 hover:text-red-400 hover:bg-neutral-800 rounded-xl transition-colors"
              >
                <Trash2 size={16} />
              </button>
            )}
          </div>
        ) : (
          <div className="flex items-center gap-1">
            <button
              onClick={onEdit}
              title="Editar persona"
              className="p-2 text-neutral-400 hover:text-white hover:bg-neutral-800 rounded-xl transition-colors"
            >
              <Edit2 size={16} />
            </button>
            <button
              onClick={onDeactivateRequest}
              title="Desactivar persona (soft-delete)"
              className="p-2 text-neutral-400 hover:text-amber-400 hover:bg-neutral-800 rounded-xl transition-colors"
            >
              <UserX size={16} />
            </button>
            {canHardDelete && (
              <button
                onClick={onHardDeleteRequest}
                title="Eliminar definitivamente (hard-delete)"
                className="p-2 text-neutral-400 hover:text-red-400 hover:bg-neutral-800 rounded-xl transition-colors"
              >
                <Trash2 size={16} />
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
