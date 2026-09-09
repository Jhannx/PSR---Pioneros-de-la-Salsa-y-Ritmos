import { useEffect, useState } from "react";
import { formatCOP } from "../../../utils/currency";
import { formatShortWeekdayDate as formatDate } from "../../../utils/date";
import { reportsService } from "../../../services/reportsService";

function isoDaysAgo(days) {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}

export default function RevenueTab({ mode = "sesion" }) {
  const [desde, setDesde] = useState(isoDaysAgo(7));
  const [hasta, setHasta] = useState(isoDaysAgo(0));
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const isDaily = mode === "diario";

  async function load() {
    setLoading(true);
    setError("");
    try {
      const data = isDaily
        ? await reportsService.getDailyRevenue(desde, hasta)
        : await reportsService.getRevenueBySession(desde, hasta);
      setRows(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(
        err.data?.message ||
          err.message ||
          `No se pudo cargar el recaudo ${isDaily ? "diario" : "por sesión"}.`
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    Promise.resolve().then(load);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const total = rows.reduce((sum, row) => sum + Number(row.totalCollected || 0), 0);

  return (
    <div>
      <div className="flex items-end gap-2 mb-4">
        <div>
          <label className="text-neutral-400 text-xs font-medium block mb-1">Desde</label>
          <input
            type="date"
            value={desde}
            onChange={(e) => setDesde(e.target.value)}
            className="bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2 text-white text-sm outline-none focus:border-emerald-500"
          />
        </div>
        <div>
          <label className="text-neutral-400 text-xs font-medium block mb-1">Hasta</label>
          <input
            type="date"
            value={hasta}
            onChange={(e) => setHasta(e.target.value)}
            className="bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2 text-white text-sm outline-none focus:border-emerald-500"
          />
        </div>
        <button
          onClick={load}
          className="bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold text-sm px-4 py-2 rounded-xl transition-colors"
        >
          Consultar
        </button>
      </div>

      {error && (
        <div className="p-4 bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm rounded-2xl mb-4">
          {error}
        </div>
      )}

      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl px-6 py-5 mb-4 flex items-center justify-between">
        <div>
          <span className="text-neutral-400 text-sm font-medium block">Total del período mostrado</span>
          <span className="text-neutral-600 text-[11px]">
            {isDaily
              ? "Caja: dinero que entró en efectivo/transferencia ese día"
              : "Devengado: atribuido a la fecha de la clase"}
          </span>
        </div>
        <span className="text-emerald-400 text-2xl font-bold">{formatCOP(total)}</span>
      </div>

      <div className="flex items-center gap-4 px-4 pb-2 text-xs font-semibold text-neutral-600 uppercase tracking-wide">
        <span className="w-32">Fecha</span>
        <span className="flex-1">Sesión</span>
        <span className="w-32 text-right">Recaudado</span>
      </div>

      <div className="flex flex-col gap-2">
        {loading ? (
          <div className="text-center py-8 text-neutral-500 text-sm">Consultando…</div>
        ) : rows.length === 0 ? (
          <div className="text-center py-8 text-neutral-500 text-sm">
            No hay recaudo registrado en este rango de fechas.
          </div>
        ) : (
          rows.map((row) => (
            <div key={`${row.date}-${row.sessionId}`} className="flex items-center gap-4 px-4 py-3.5 bg-neutral-900 border border-neutral-800 rounded-2xl">
              <span className="w-32 text-white font-semibold capitalize">{formatDate(row.date)}</span>
              <span className="flex-1 text-neutral-400 text-sm">Sesión #{row.sessionId}</span>
              <span className="w-32 text-right text-neutral-200 font-semibold">{formatCOP(row.totalCollected)}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
