import { useState } from "react";
import { CheckCircle2, CreditCard, KeyRound } from "lucide-react";
import Field from "../../components/forms/Field";

const inputClass =
  "w-full bg-neutral-900 border border-neutral-800 rounded-xl px-4 py-3 text-white text-base placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

// documento llega precargado desde vista_checkin_kiosko.jsx — la persona
// ya lo digitó una vez, no se le vuelve a pedir.
//
// Nota: tipo de sangre y teléfono de emergencia se quitaron de este
// formulario — esos datos ahora viven en un paso/pantalla de contacto de
// emergencia aparte (ver vista_exploracion_contacto_emergencia.jsx),
// todavía pendiente de decidir el formato final (texto libre vs chips).
export default function LobbyRegistrationPage({ documento = "1005678901", activityName = "Aeróbicos" }) {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");
  const [passwordMode, setPasswordMode] = useState("documento"); // documento | propia
  const [password, setPassword] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [queuePosition] = useState(5); // mock: posición asignada al terminar el registro

  const canSubmit =
    firstName.trim() && lastName.trim() && phone.trim() && (passwordMode === "documento" || password.trim());

  function handleSubmit() {
    if (!canSubmit) return;
    // Sin paso de aprobación: al guardar, la persona queda registrada
    // (con su cuenta de acceso creada en el mismo paso) y entra directo
    // a la cola de ingreso.
    setSubmitted(true);
  }

  if (submitted) {
    return (
      <div className="min-h-screen w-full bg-neutral-950 flex items-center justify-center px-6">
        <div className="w-full max-w-md text-center">
          <div className="w-16 h-16 rounded-2xl bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-6">
            <CheckCircle2 className="text-emerald-400" size={28} />
          </div>
          <h1 className="text-2xl font-bold text-white mb-2">¡Listo, {firstName}!</h1>
          <p className="text-neutral-400 mb-6">Ya quedaste registrado y estás en la fila de hoy</p>

          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 text-left">
            <p className="text-white font-semibold">
              {firstName} {lastName}
            </p>
            <p className="text-neutral-500 text-sm mt-0.5">Documento: {documento}</p>
            <p className="text-neutral-500 text-sm">{phone}</p>
          </div>

          <p className="text-neutral-500 text-sm mt-6">
            Turno número <span className="text-white font-bold">{queuePosition}</span> — mira la pantalla de sala
            de espera
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-center justify-center px-6 py-10">
      <div className="w-full max-w-2xl">
        <div className="text-center mb-6">
          <p className="text-neutral-500 text-sm font-semibold tracking-wide uppercase">{activityName}</p>
          <h1 className="text-2xl font-bold text-white mt-1">Crea tu perfil</h1>
          <p className="text-neutral-400 text-sm mt-1">Completa tus datos para poder ingresar a las clases</p>
        </div>

        <div className="bg-neutral-900/60 border border-neutral-800 rounded-3xl p-6">
          <div className="flex items-center gap-2 bg-neutral-800/60 border border-neutral-800 rounded-xl px-4 py-3 mb-5">
            <CreditCard size={16} className="text-neutral-500" />
            <span className="text-neutral-500 text-sm">Documento</span>
            <span className="text-white font-semibold text-sm ml-auto">{documento}</span>
          </div>

          <div className="grid grid-cols-2 gap-4 mb-4">
            <Field label="Nombre">
              <input
                autoFocus
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
          </div>

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

          {/* Elección de contraseña — por defecto es el documento, la
              persona puede optar por la suya propia. */}
          <div className="border-t border-neutral-800 mt-4 pt-4">
            <label className="text-neutral-300 text-sm font-semibold flex items-center gap-1.5 mb-2">
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
              <p className="text-neutral-500 text-xs">
                Tu contraseña será tu documento:{" "}
                <span className="text-white font-semibold">{documento}</span>. La puedes cambiar después desde tu
                perfil.
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
          className="w-full mt-6 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 transition-colors text-neutral-950 font-bold text-lg py-4 rounded-2xl"
        >
          Guardar y entrar a la fila
        </button>
      </div>
    </div>
  );
}
