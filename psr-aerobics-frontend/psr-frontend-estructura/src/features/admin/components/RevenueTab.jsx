import { formatCOP } from "../../../utils/currency";
import { formatShortWeekdayDate as formatDate } from "../../../utils/date";

const initialRevenue = [
  { date: "2026-08-27", sessionId: 41, totalCollected: 72000 },
  { date: "2026-08-27", sessionId: 42, totalCollected: 36000 },
  { date: "2026-08-26", sessionId: 40, totalCollected: 44000 },
  { date: "2026-08-25", sessionId: 39, totalCollected: 0 },
  { date: "2026-08-24", sessionId: 38, totalCollected: 88000 },
];

export default function RevenueTab() {
  const total = initialRevenue.reduce((sum, row) => sum + row.totalCollected, 0);

  return (
    <div>
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl px-6 py-5 mb-4 flex items-center justify-between">
        <span className="text-neutral-400 text-sm font-medium">Total del período mostrado</span>
        <span className="text-emerald-400 text-2xl font-bold">{formatCOP(total)}</span>
      </div>

      <div className="flex items-center gap-4 px-4 pb-2 text-xs font-semibold text-neutral-600 uppercase tracking-wide">
        <span className="w-32">Fecha</span>
        <span className="flex-1">Sesión</span>
        <span className="w-32 text-right">Recaudado</span>
      </div>

      <div className="flex flex-col gap-2">
        {initialRevenue.map((row) => (
          <div key={`${row.date}-${row.sessionId}`} className="flex items-center gap-4 px-4 py-3.5 bg-neutral-900 border border-neutral-800 rounded-2xl">
            <span className="w-32 text-white font-semibold capitalize">{formatDate(row.date)}</span>
            <span className="flex-1 text-neutral-400 text-sm">Sesión #{row.sessionId}</span>
            <span className="w-32 text-right text-neutral-200 font-semibold">{formatCOP(row.totalCollected)}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
