import { formatDayParts } from "./weekUtils";

// Franja de UN día en la vista por semana: a la izquierda el bloque de fecha (día de la semana y
// número) y a la derecha las sesiones de ese día. El día de HOY va resaltado en verde con la marca
// "Hoy", para ubicarse de un vistazo (docs/correciones 20-09.md, 2ª ronda: "que marque que
// estamos en este día"); los días pasados se atenúan. Es solo presentación: saber qué día es hoy no
// es lógica de negocio.
export default function SessionDayBand({ dayKey, isToday, isPast, children, emptyText = "Sin sesiones" }) {
  const { weekdayShort, weekday, day, month } = formatDayParts(dayKey);
  const hasChildren = Boolean(children) && (!Array.isArray(children) || children.length > 0);

  return (
    <section
      aria-label={`${weekday} ${day} de ${month}`}
      className={`rounded-2xl border overflow-hidden ${
        isToday
          ? "border-emerald-500/60 bg-emerald-500/[0.05] shadow-lg shadow-emerald-500/10"
          : "border-neutral-800 bg-neutral-900/40"
      } ${isPast && !isToday ? "opacity-70" : ""}`}
    >
      <div className="flex flex-col md:flex-row">
        <div
          className={`md:w-36 shrink-0 px-4 py-3 md:py-5 flex md:flex-col items-center md:items-start justify-between md:justify-center gap-2 ${
            isToday ? "bg-emerald-500 text-neutral-950" : "bg-neutral-900 text-neutral-300 md:border-r border-neutral-800"
          }`}
        >
          <div className="flex md:block items-baseline gap-2">
            <p className="text-[11px] font-extrabold uppercase tracking-widest opacity-80">{weekdayShort}</p>
            <p className="text-3xl font-extrabold leading-none md:mt-0.5">{day}</p>
            <p className="text-xs font-semibold opacity-80 md:mt-0.5">{month}</p>
          </div>
          {isToday && (
            <span className="text-[10px] font-extrabold uppercase tracking-wider bg-neutral-950/20 px-2.5 py-1 rounded-full">
              Hoy
            </span>
          )}
        </div>

        <div className="flex-1 p-3 md:p-4 min-w-0">
          {hasChildren ? (
            children
          ) : (
            <p className="text-neutral-600 text-sm py-2 md:py-0 md:h-full md:flex md:items-center">{emptyText}</p>
          )}
        </div>
      </div>
    </section>
  );
}
