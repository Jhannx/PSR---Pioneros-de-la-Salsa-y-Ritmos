import { useState } from "react";
import { ArrowRight, History } from "lucide-react";
import { formatDateTimeShort as formatDateTime } from "../../utils/date";

const TABLE_LABELS = {
  ASISTENCIA: "Asistencia",
  MOVIMIENTO_CUENTA: "Movimiento de cuenta",
  TARIFA: "Tarifa",
  CONFIGURACION_COLA: "Configuración de cola",
  PERSONA: "Persona",
  SESION: "Sesión",
};

const FILTERS = [
  { key: "all", label: "Todo" },
  { key: "TARIFA", label: "Tarifa" },
  { key: "MOVIMIENTO_CUENTA", label: "Pagos" },
  { key: "ASISTENCIA", label: "Asistencia" },
  { key: "PERSONA", label: "Personas" },
  { key: "SESION", label: "Sesiones" },
  { key: "CONFIGURACION_COLA", label: "Cola" },
];

const initialHistory = [
  { id: 300, affectedTable: "TARIFA", field: "monto", oldValue: "4000.00", newValue: "4500.00", performedBy: "Camilo Pérez", dateTime: "2026-08-27T08:00:00" },
  { id: 299, affectedTable: "SESION", field: "cancelada", oldValue: "false", newValue: "true", performedBy: "Laura Gómez", dateTime: "2026-08-25T05:55:00" },
  { id: 298, affectedTable: "PERSONA", field: "persona", oldValue: "activa", newValue: "eliminada_definitivamente", performedBy: "Camilo Pérez", dateTime: "2026-08-24T14:10:00" },
  { id: 297, affectedTable: "MOVIMIENTO_CUENTA", field: "monto_abonado", oldValue: "4000.00", newValue: "3000.00", performedBy: "Camilo Pérez", dateTime: "2026-08-22T09:41:00" },
  { id: 296, affectedTable: "PERSONA", field: "activo", oldValue: "false", newValue: "true", performedBy: "Laura Gómez", dateTime: "2026-08-20T11:03:00" },
  { id: 295, affectedTable: "CONFIGURACION_COLA", field: "personas_por_turno", oldValue: "1", newValue: "2", performedBy: "Camilo Pérez", dateTime: "2026-08-18T16:00:00" },
];

export default function AuditPage() {
  const [activeFilter, setActiveFilter] = useState("all");

  const filtered =
    activeFilter === "all" ? initialHistory : initialHistory.filter((row) => row.affectedTable === activeFilter);

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-6 py-8">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center gap-3 mb-1">
          <History className="text-neutral-500" size={22} />
          <h1 className="text-2xl font-bold text-white">Auditoría</h1>
        </div>
        <p className="text-neutral-500 text-sm mb-6">
          Historial de cambios sobre información sensible del sistema. Solo lectura.
        </p>

        <div className="flex flex-wrap gap-2 mb-5">
          {FILTERS.map((f) => {
            const active = activeFilter === f.key;
            return (
              <button
                key={f.key}
                onClick={() => setActiveFilter(f.key)}
                className={`px-3.5 py-2 rounded-full text-sm font-semibold border transition-colors ${
                  active
                    ? "bg-white text-neutral-950 border-white"
                    : "bg-neutral-900 text-neutral-400 border-neutral-800 hover:border-neutral-700"
                }`}
              >
                {f.label}
              </button>
            );
          })}
        </div>

        <div className="flex flex-col gap-2">
          {filtered.map((row) => (
            <div key={row.id} className="flex items-center gap-4 px-4 py-3.5 bg-neutral-900 border border-neutral-800 rounded-2xl">
              <span className="w-40 shrink-0">
                <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-neutral-800 text-neutral-400">
                  {TABLE_LABELS[row.affectedTable]}
                </span>
              </span>
              <span className="w-40 shrink-0 text-neutral-400 text-sm">{row.field}</span>
              <span className="flex-1 flex items-center gap-2 text-sm min-w-0">
                <span className="text-neutral-600 truncate">{row.oldValue ?? "—"}</span>
                <ArrowRight size={13} className="text-neutral-700 shrink-0" />
                <span className="text-white font-semibold truncate">{row.newValue ?? "—"}</span>
              </span>
              <span className="w-40 shrink-0 text-right text-neutral-400 text-sm">{row.performedBy}</span>
              <span className="w-32 shrink-0 text-right text-neutral-600 text-xs">{formatDateTime(row.dateTime)}</span>
            </div>
          ))}
          {filtered.length === 0 && (
            <div className="text-center py-10 text-neutral-600 text-sm">No hay cambios registrados en esta categoría.</div>
          )}
        </div>
      </div>
    </div>
  );
}
