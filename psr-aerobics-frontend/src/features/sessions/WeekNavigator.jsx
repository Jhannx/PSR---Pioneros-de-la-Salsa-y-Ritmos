import { ChevronLeft, ChevronRight } from "lucide-react";
import { formatRangeLabel } from "./weekUtils";

// Selector de semana (lunes a domingo): anterior, siguiente y "Esta semana". Es solo el control; el
// que lo usa decide qué pedirle al servidor con el rango.
export default function WeekNavigator({ offset, onChange, desde, hasta }) {
  return (
    <div className="flex items-center gap-1.5">
      <button
        type="button"
        onClick={() => onChange(offset - 1)}
        aria-label="Semana anterior"
        className="p-1.5 rounded-lg border border-neutral-800 text-neutral-400 hover:text-white hover:border-neutral-700 transition-colors"
      >
        <ChevronLeft size={16} />
      </button>
      <span className="text-sm font-semibold text-white min-w-[10.5rem] text-center">
        {formatRangeLabel(desde, hasta)}
      </span>
      <button
        type="button"
        onClick={() => onChange(offset + 1)}
        aria-label="Semana siguiente"
        className="p-1.5 rounded-lg border border-neutral-800 text-neutral-400 hover:text-white hover:border-neutral-700 transition-colors"
      >
        <ChevronRight size={16} />
      </button>
      {offset !== 0 && (
        <button
          type="button"
          onClick={() => onChange(0)}
          className="ml-1 text-xs font-semibold text-emerald-400 hover:text-emerald-300 underline underline-offset-2"
        >
          Esta semana
        </button>
      )}
    </div>
  );
}
