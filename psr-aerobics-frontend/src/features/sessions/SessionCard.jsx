import { Users, CheckCircle2, Flag } from "lucide-react";
import { formatTimeOfDay } from "../../utils/date";
import SessionStatusBadge from "./SessionStatusBadge";

// Tarjeta de una sesión de clase. Qué se ofrece (finalizar, cancelar) lo dicen las banderas que calcula
// el servidor según quién pregunta (`canClose`, `canCancel`, NOTAS-PARA-FRONTEND.md Bloque 5, punto 6):
// la tarjeta no compara horas ni roles. Los totales y el nombre del profesor también vienen en la sesión.
export default function SessionCard({
  session,
  teacherLabel,
  teachersList,
  isReassigning,
  reassignSaving,
  reassignError,
  closing,
  onStartReassign,
  onCancelReassign,
  onReassign,
  onClose,
  onCancel,
}) {
  const isCancelled = session.status === "CANCELLED" || session.cancelled;
  const inProgress = session.status === "IN_PROGRESS";
  const canClose = session.canClose === true;
  const canCancel = session.canCancel === true;

  return (
    <div
      className={`bg-neutral-900 border rounded-xl p-4 flex flex-col justify-between gap-3 transition-colors ${
        isCancelled ? "border-red-500/30 opacity-75" : inProgress ? "border-emerald-500/50" : "border-neutral-800"
      }`}
    >
      <div>
        <div className="flex items-start justify-between gap-2 mb-2">
          <h3 className="text-lg font-bold text-white leading-tight">
            {formatTimeOfDay(session.startTime)} - {formatTimeOfDay(session.endTime)}
          </h3>
          <SessionStatusBadge status={isCancelled ? "CANCELLED" : session.status} />
        </div>

        {isReassigning ? (
          <div>
            <select
              autoFocus
              defaultValue=""
              disabled={reassignSaving}
              onChange={(e) => onReassign(e.target.value)}
              className="w-full bg-neutral-800 border border-neutral-700 rounded-lg px-2 py-1.5 text-white text-xs outline-none focus:border-emerald-500"
            >
              <option value="" disabled>
                {reassignSaving ? "Guardando…" : "Elegir profesor…"}
              </option>
              {teachersList.map((t) => (
                <option key={t.userId || t.id} value={t.userId || t.id}>
                  {t.fullName || `${t.firstName} ${t.lastName}`}
                </option>
              ))}
            </select>
            <button
              onClick={onCancelReassign}
              disabled={reassignSaving}
              className="text-neutral-500 hover:text-white text-[11px] mt-1"
            >
              Cancelar
            </button>
            {reassignError && <p className="text-rose-400 text-[11px] mt-1">{reassignError}</p>}
          </div>
        ) : (
          <p className="text-sm text-neutral-400">
            Profesor(a): <span className="text-neutral-200">{teacherLabel}</span>
            {!isCancelled && (
              <button
                onClick={onStartReassign}
                className="ml-2 text-emerald-400 hover:text-emerald-300 text-xs font-semibold underline underline-offset-2"
              >
                {session.teacherUserId ? "Cambiar" : "Asignar"}
              </button>
            )}
          </p>
        )}
      </div>

      <div className="pt-3 border-t border-neutral-800/80 flex items-center justify-between gap-2 text-xs text-neutral-400 flex-wrap">
        <div className="flex gap-3">
          <span className="flex items-center gap-1">
            <Users size={14} className="text-neutral-500" /> {session.totalCheckins || 0}
          </span>
          <span className="flex items-center gap-1">
            <CheckCircle2 size={14} className="text-emerald-400" /> {session.totalAttended || 0} asistieron
          </span>
        </div>

        <div className="flex items-center gap-3">
          {canClose && (
            <button
              onClick={onClose}
              disabled={closing}
              className="flex items-center gap-1 text-amber-400 hover:text-amber-300 font-semibold transition-colors disabled:opacity-50"
            >
              <Flag size={13} />
              {closing ? "Finalizando…" : "Finalizar"}
            </button>
          )}
          {canCancel && (
            <button onClick={onCancel} className="text-red-400 hover:text-red-300 font-semibold transition-colors">
              Cancelar
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
