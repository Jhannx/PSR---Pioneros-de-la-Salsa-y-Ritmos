import { useState, useEffect } from "react";
import { ArrowRight, History } from "lucide-react";
import { formatDateTimeShort as formatDateTime } from "../../utils/date";
import { reportsService } from "../../services/reportsService";

const TABLE_LABELS = {
  ATTENDANCE: "Asistencia",
  ACCOUNT_MOVEMENT: "Movimiento de cuenta",
  PRICING: "Tarifa",
  QUEUE_CONFIG: "Configuración de cola",
  PERSON: "Persona",
  SESSION: "Sesión",
};

const FILTERS = [
  { key: "all", label: "Todo" },
  { key: "PRICING", label: "Tarifa" },
  { key: "ACCOUNT_MOVEMENT", label: "Pagos" },
  { key: "ATTENDANCE", label: "Asistencia" },
  { key: "PERSON", label: "Personas" },
  { key: "SESSION", label: "Sesiones" },
  { key: "QUEUE_CONFIG", label: "Cola" },
];

export default function AuditPage() {
  const [activeFilter, setActiveFilter] = useState("all");
  const [history, setHistory] = useState([]);
  const [error, setError] = useState("");
  const [from, setFrom] = useState(() => `${new Date().toISOString().slice(0, 10)}T00:00`);
  const [to, setTo] = useState(() => `${new Date().toISOString().slice(0, 10)}T23:59`);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    async function loadAudit() {
      try {
        setError("");
        const response = await reportsService.getAuditHistory({
          desde: `${from}:00-05:00`, hasta: `${to}:00-05:00`, page, size: 25,
        });
        if (response && Array.isArray(response.content)) {
          setHistory(response.content);
          setTotalPages(response.totalPages || 0);
        } else if (Array.isArray(response)) {
          setHistory(response);
        }
      } catch (err) {
        setHistory([]);
        setError(err.data?.message || err.message || "No fue posible cargar el historial.");
      }
    }
    loadAudit();
  }, [from, to, page]);

  const filtered =
    activeFilter === "all" ? history : history.filter((row) => row.affectedTable === activeFilter);

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-4 md:px-6 py-4 text-white font-body">
      <div className="max-w-6xl mx-auto space-y-3">
        <div className="border-b border-neutral-800 pb-3">
          <div className="flex items-center gap-2">
            <History className="text-neutral-400" size={18} />
            <h1 className="text-xl font-bold text-white tracking-tight">Auditoría de Actividad</h1>
          </div>
          <p className="text-neutral-400 text-xs mt-0.5">
            Historial de cambios sobre información sensible del sistema (Solo lectura)
          </p>
        </div>

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
        <div className="flex flex-wrap gap-3 items-end mb-4">
          <label className="text-xs text-neutral-400">Desde<input type="datetime-local" value={from} onChange={(e) => { setPage(0); setFrom(e.target.value); }} className="block mt-1 bg-neutral-900 border border-neutral-700 rounded-lg p-2 text-white" /></label>
          <label className="text-xs text-neutral-400">Hasta<input type="datetime-local" value={to} onChange={(e) => { setPage(0); setTo(e.target.value); }} className="block mt-1 bg-neutral-900 border border-neutral-700 rounded-lg p-2 text-white" /></label>
        </div>
        {error && <div className="mb-3 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm">{error}</div>}

        <div className="flex flex-col gap-2">
          {filtered.map((row) => (
            <div key={row.id} className="flex items-center gap-4 px-4 py-3.5 bg-neutral-900 border border-neutral-800 rounded-2xl">
              <span className="w-40 shrink-0">
                <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-neutral-800 text-neutral-400">
                  {TABLE_LABELS[row.affectedTable] || row.affectedTable}
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
        {totalPages > 1 && <div className="flex justify-center gap-3 text-sm"><button disabled={page === 0} onClick={() => setPage((current) => current - 1)} className="px-3 py-2 bg-neutral-800 rounded-lg disabled:opacity-40">Anterior</button><span className="py-2 text-neutral-400">Página {page + 1} de {totalPages}</span><button disabled={page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)} className="px-3 py-2 bg-neutral-800 rounded-lg disabled:opacity-40">Siguiente</button></div>}
      </div>
    </div>
  );
}
