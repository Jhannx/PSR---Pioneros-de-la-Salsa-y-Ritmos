// Repetido idéntico en vista_registro_movil.jsx, vista_registro_kiosko.jsx
// y vista_contacto_emergencia_movil.jsx — envoltorio de label + ícono
// para un campo de formulario.
export default function Field({ label, hint, icon, children }) {
  return (
    <div>
      <label className="text-neutral-300 text-sm font-semibold flex items-center gap-1.5 mb-1.5">
        {icon}
        {label}
      </label>
      {children}
      {hint && <p className="text-neutral-600 text-xs mt-1.5">{hint}</p>}
    </div>
  );
}
