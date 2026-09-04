import { useState } from "react";
import { CheckCircle2, CreditCard, KeyRound } from "lucide-react";
import Field from "../../components/forms/Field";

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-4 py-3 text-white text-base placeholder-neutral-600 outline-none focus:border-emerald-500";

// Nota: tipo de sangre y teléfono de emergencia se quitaron de este
// formulario — esos datos ahora viven en un paso/pantalla de contacto de
// emergencia aparte, todavía pendiente de decidir el formato final.
//
// Nota: documento es nuevo aquí — antes este formulario no lo pedía.
// Ahora hace falta porque la contraseña por defecto de la cuenta que se
// crea en este mismo paso ES el documento.
export default function SelfRegistrationPage({ activityName = "Aeróbicos" }) {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [documento, setDocumento] = useState("");
  const [phone, setPhone] = useState("");
  const [passwordMode, setPasswordMode] = useState("documento"); // documento | propia
  const [password, setPassword] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const canSubmit =
    firstName.trim() &&
    lastName.trim() &&
    documento.trim() &&
    phone.trim() &&
    (passwordMode === "documento" || password.trim());

  function handleSubmit() {
    if (!canSubmit) return;
    setSubmitted(true);
  }

  if (submitted) {
    return (
      <div className="min-h-screen w-full bg-neutral-950 flex items-start justify-center px-4 py-10">
        <div className="w-full max-w-sm">
          <div className="bg-neutral-900 rounded-3xl p-6 border border-neutral-800 text-center">
            <div className="w-16 h-16 rounded-full bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-4">
              <CheckCircle2 className="text-emerald-400" size={32} />
            </div>
            <h1 className="text-xl font-bold text-white">¡Listo!</h1>
            <p className="text-neutral-400 text-sm mt-2">
              Ya quedaste registrado. La próxima vez que escanees el QR de asistencia, puedes marcar directo.
            </p>

            <div className="mt-6 bg-neutral-800 rounded-2xl p-4 text-left">
              <p className="text-white font-semibold">
                {firstName} {lastName}
              </p>
              <p className="text-neutral-500 text-sm mt-0.5">Documento: {documento}</p>
              <p className="text-neutral-500 text-sm">{phone}</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-start justify-center px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="text-center mb-6">
          <p className="text-neutral-500 text-sm font-semibold tracking-wide uppercase">{activityName}</p>
          <h1 className="text-2xl font-bold text-white mt-1">Crea tu perfil</h1>
          <p className="text-neutral-400 text-sm mt-1">
            Completa tus datos para poder ingresar a las clases
          </p>
        </div>

        <div className="bg-neutral-900 rounded-3xl p-6 border border-neutral-800 flex flex-col gap-4">
          <Field label="Nombre">
            <input
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              placeholder="Rosa"
              className={inputClass}
            />
          </Field>

          <Field label="Apellidos">
            <input
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              placeholder="Martínez López"
              className={inputClass}
            />
          </Field>

          <Field label="Documento" icon={<CreditCard size={14} />}>
            <input
              inputMode="numeric"
              value={documento}
              onChange={(e) => setDocumento(e.target.value.replace(/\D/g, ""))}
              placeholder="1001234567"
              className={inputClass}
            />
          </Field>

          <Field label="Teléfono">
            <input
              type="tel"
              inputMode="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="300 123 4567"
              className={inputClass}
            />
          </Field>

          {/* Elección de contraseña — por defecto es el documento. */}
          <div>
            <label className="text-neutral-300 text-sm font-semibold flex items-center gap-1.5 mb-1.5">
              <KeyRound size={14} />
              Tu contraseña
            </label>
            <div className="flex bg-neutral-800 rounded-lg p-1 text-xs font-semibold mb-2">
              <button
                onClick={() => setPasswordMode("documento")}
                className={`flex-1 px-3 py-2 rounded-md transition-colors ${
                  passwordMode === "documento" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400"
                }`}
              >
                Usar mi documento
              </button>
              <button
                onClick={() => setPasswordMode("propia")}
                className={`flex-1 px-3 py-2 rounded-md transition-colors ${
                  passwordMode === "propia" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400"
                }`}
              >
                Elegir la mía
              </button>
            </div>
            {passwordMode === "documento" ? (
              <p className="text-neutral-600 text-xs">
                Será tu documento{documento ? `: ${documento}` : ""}. La puedes cambiar después desde tu perfil.
              </p>
            ) : (
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Escribe tu contraseña"
                className={inputClass}
              />
            )}
          </div>
        </div>

        <button
          onClick={handleSubmit}
          disabled={!canSubmit}
          className="w-full mt-6 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 active:scale-[0.98] transition-all text-neutral-950 font-bold text-lg py-5 rounded-2xl shadow-lg shadow-emerald-500/20"
        >
          Enviar solicitud
        </button>
      </div>
    </div>
  );
}
