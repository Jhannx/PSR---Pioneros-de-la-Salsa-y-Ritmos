// Panel que aparece SOLO cuando falta un consentimiento puntual (salud o terceros) al intentar
// una acción específica — no se muestra mientras la persona solo está escribiendo. Muestra la
// cita literal de la sección correspondiente de la política (no una paráfrasis) y un enlace a la
// política completa, para que la persona sepa exactamente qué está aceptando antes de marcarlo.
//
// `excerpt` viene de constants/policyExcerpts.js. `context` describe en qué acción se está —
// "agregar este contacto" / "guardar tus observaciones de salud" — para armar la frase de arriba.
export default function MissingConsentPanel({ excerpt, context, checked, onChange }) {
  return (
    <div className="border border-amber-500/30 bg-amber-500/5 rounded-xl p-4 flex flex-col gap-3">
      <p className="text-amber-400 text-xs font-semibold leading-relaxed">
        Para {context}, primero debes aceptar esto (política de datos, numeral {excerpt.numeral}):
      </p>

      <blockquote className="border-l-2 border-emerald-500/40 bg-neutral-950/50 rounded-r-lg pl-3 pr-2.5 py-2 text-neutral-300 text-xs italic leading-relaxed">
        {excerpt.quote}
      </blockquote>

      <label className="flex items-start gap-2.5 cursor-pointer">
        <input
          type="checkbox"
          checked={checked}
          onChange={(e) => onChange(e.target.checked)}
          className="mt-0.5 w-4 h-4 shrink-0 accent-emerald-500 cursor-pointer"
        />
        <span className="text-neutral-300 text-xs leading-relaxed">{excerpt.checkboxLabel}</span>
      </label>

      <a
        href={`/politica-privacidad#${excerpt.anchor}`}
        target="_blank"
        rel="noopener noreferrer"
        className="self-start text-emerald-400 hover:text-emerald-300 underline underline-offset-2 text-[11px]"
      >
        Leer política de datos completa →
      </a>
    </div>
  );
}
