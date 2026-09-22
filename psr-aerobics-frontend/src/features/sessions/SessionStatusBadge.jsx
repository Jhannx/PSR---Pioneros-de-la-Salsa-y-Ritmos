// Etiqueta del estado de una sesión, tal como lo calcula el servidor con su propio reloj
// (`status`, NOTAS-PARA-FRONTEND.md Bloque 4, punto 5.3): UPCOMING (aún no empieza), IN_PROGRESS,
// ENDED (ya pasó su hora fin) y CANCELLED (gana siempre). El frontend no compara horas para saberlo.
const STATUS_STYLES = {
  UPCOMING: { label: "Programada", className: "text-sky-300 bg-sky-500/10 border-sky-500/20" },
  IN_PROGRESS: { label: "En curso", className: "text-emerald-400 bg-emerald-500/10 border-emerald-500/20" },
  ENDED: { label: "Terminada", className: "text-neutral-400 bg-neutral-800 border-neutral-700" },
  CANCELLED: { label: "Cancelada", className: "text-red-400 bg-red-500/10 border-red-500/20" },
};

export default function SessionStatusBadge({ status }) {
  const style = STATUS_STYLES[status];
  if (!style) return null;
  return (
    <span className={`text-xs font-semibold border px-2.5 py-0.5 rounded-md ${style.className}`}>
      {style.label}
    </span>
  );
}
