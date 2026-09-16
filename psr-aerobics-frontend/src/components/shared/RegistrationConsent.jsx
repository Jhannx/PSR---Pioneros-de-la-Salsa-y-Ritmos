// Checkbox de aceptación general + checkbox de declaración de participación voluntaria
// (docs/pendiente-implementar-sistema.md §Frontend #2, política numeral 12) — dos declaraciones
// legales distintas, van juntas en el mismo paso pero deliberadamente en dos casillas separadas,
// no fusionadas en una: una es aceptación del tratamiento de datos, la otra es una renuncia de
// responsabilidad sobre el riesgo físico de la actividad. Marcarlas juntas escondería cuál de las
// dos se está aceptando.
//
// Alcance confirmado igual que RegistrationConfirmModal.jsx: solo SelfRegistrationPage y
// LobbyRegistrationPage (los dos flujos de autoregistro digital). No aplica a PersonFormModal —
// el alta que hace recepción usa un formato físico firmado en vez del checkbox digital
// (docs/pendiente-implementar-sistema.md, "Decisiones de negocio pendientes" #2).
export default function RegistrationConsent({
  acceptedPolicy,
  onAcceptedPolicyChange,
  acceptedVoluntary,
  onAcceptedVoluntaryChange,
}) {
  return (
    <div className="border-t border-neutral-800 mt-4 pt-4 flex flex-col gap-3">
      <label className="flex items-start gap-2.5 cursor-pointer">
        <input
          type="checkbox"
          checked={acceptedPolicy}
          onChange={(e) => onAcceptedPolicyChange(e.target.checked)}
          className="mt-0.5 w-4 h-4 shrink-0 accent-emerald-500 cursor-pointer"
        />
        <span className="text-neutral-400 text-xs leading-relaxed">
          He leído y acepto la{" "}
          <a
            href="/politica-privacidad"
            target="_blank"
            rel="noopener noreferrer"
            className="text-emerald-400 hover:text-emerald-300 underline underline-offset-2"
          >
            Política de Tratamiento de Datos Personales
          </a>{" "}
          de PSR.
        </span>
      </label>

      <label className="flex items-start gap-2.5 cursor-pointer">
        <input
          type="checkbox"
          checked={acceptedVoluntary}
          onChange={(e) => onAcceptedVoluntaryChange(e.target.checked)}
          className="mt-0.5 w-4 h-4 shrink-0 accent-emerald-500 cursor-pointer"
        />
        <span className="text-neutral-400 text-xs leading-relaxed">
          Entiendo que la actividad física conlleva riesgo de lesión y participo de manera
          voluntaria.
        </span>
      </label>
    </div>
  );
}
