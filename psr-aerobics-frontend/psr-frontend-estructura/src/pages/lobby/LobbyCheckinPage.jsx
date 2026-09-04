import { useState } from "react";
import { CreditCard, AlertCircle, RotateCcw, UserPlus, CheckCircle2, Users } from "lucide-react";
import { formatTimeWithSeconds as formatTime } from "../../utils/date";

// Mock de documentos ya registrados — solo para simular la búsqueda del
// kiosko. En producción esto es una consulta al backend (RF-03, método
// Documento).
const KNOWN_DOCUMENTS = ["1001234567", "1002345678", "1003456789", "1004567890"];

export default function LobbyCheckinPage() {
  const [step, setStep] = useState("documento"); // documento | no_encontrado | confirmado | redirigiendo
  const [documento, setDocumento] = useState("");
  // Hora exacta en la que la persona dio "Aceptar" — es el dato objetivo
  // que define su posición real en la fila, sin depender de percepciones
  // ("yo llegué primero"). Se registra en el momento del click, no después.
  const [acceptedAt, setAcceptedAt] = useState(null);
  const [queuePosition] = useState(4); // mock: posición asignada al ingresar a la cola

  function handleSubmit() {
    const trimmed = documento.trim();
    if (!trimmed) return;
    if (KNOWN_DOCUMENTS.includes(trimmed)) {
      setAcceptedAt(new Date());
      setStep("confirmado");
    } else {
      setStep("no_encontrado");
    }
  }

  function handleRetry() {
    setDocumento("");
    setStep("documento");
  }

  function handleCreateUser() {
    // En producción esto navega a vista_registro_kiosko.jsx pasando el
    // documento ya digitado, para no volver a pedirlo.
    setStep("redirigiendo");
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-center justify-center px-6">
      <div className="w-full max-w-lg">
        {step === "documento" && (
          <div className="text-center">
            <div className="w-16 h-16 rounded-2xl bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-6">
              <CreditCard className="text-emerald-400" size={28} />
            </div>
            <h1 className="text-3xl font-bold text-white mb-2">Ingresa tu número de documento</h1>
            <p className="text-neutral-400 mb-8">Para hacer check-in en la fila de hoy</p>

            <input
              autoFocus
              value={documento}
              onChange={(e) => setDocumento(e.target.value.replace(/\D/g, ""))}
              onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
              placeholder="Ej. 1001234567"
              inputMode="numeric"
              className="w-full bg-neutral-900 border-2 border-neutral-800 rounded-2xl px-5 py-5 text-white text-2xl text-center tracking-widest placeholder-neutral-700 outline-none focus:border-emerald-500 transition-colors"
            />

            <button
              onClick={handleSubmit}
              disabled={!documento.trim()}
              className="w-full mt-4 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 transition-colors text-neutral-950 font-bold text-lg py-4 rounded-2xl"
            >
              Aceptar
            </button>
          </div>
        )}

        {step === "no_encontrado" && (
          <div className="text-center">
            <div className="w-16 h-16 rounded-2xl bg-red-500/15 border-2 border-red-500 flex items-center justify-center mx-auto mb-6">
              <AlertCircle className="text-red-400" size={28} />
            </div>
            <h1 className="text-2xl font-bold text-white mb-2">No tienes usuario registrado</h1>
            <p className="text-neutral-400 mb-8">
              Puede que hayas digitado mal tu documento, o que sea tu primera vez aquí.
            </p>

            <div className="flex flex-col gap-3">
              <button
                onClick={handleRetry}
                className="flex items-center justify-center gap-2 border-2 border-neutral-700 text-neutral-200 hover:bg-neutral-800 font-semibold text-base py-4 rounded-2xl transition-colors"
              >
                <RotateCcw size={18} />
                Volver a intentar
              </button>
              <button
                onClick={handleCreateUser}
                className="flex items-center justify-center gap-2 bg-white text-neutral-950 hover:bg-neutral-200 font-bold text-base py-4 rounded-2xl transition-colors"
              >
                <UserPlus size={18} />
                Crear usuario
              </button>
            </div>
          </div>
        )}

        {step === "confirmado" && (
          <div className="text-center">
            <div className="w-16 h-16 rounded-2xl bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-6">
              <CheckCircle2 className="text-emerald-400" size={28} />
            </div>
            <h1 className="text-2xl font-bold text-white mb-2">Ya estás en la fila</h1>
            <p className="text-neutral-400 mb-6">Mira la pantalla de sala de espera para ver tu turno</p>

            <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 flex items-center justify-center gap-4">
              <Users className="text-neutral-500" size={20} />
              <p className="text-neutral-300 text-sm">
                Turno número <span className="text-white font-bold text-lg">{queuePosition}</span>
              </p>
            </div>
            {acceptedAt && (
              <p className="text-neutral-600 text-xs mt-4">
                Hora de ingreso a la fila: {formatTime(acceptedAt)}
              </p>
            )}
          </div>
        )}

        {step === "redirigiendo" && (
          <div className="text-center">
            <div className="w-16 h-16 rounded-2xl bg-neutral-800 border-2 border-neutral-700 flex items-center justify-center mx-auto mb-6">
              <UserPlus className="text-neutral-400" size={28} />
            </div>
            <h1 className="text-xl font-bold text-white mb-2">Redirigiendo a registro...</h1>
            <p className="text-neutral-500 text-sm">
              (En producción esto abre{" "}
              <code className="text-neutral-400">vista_registro_kiosko.jsx</code> con tu documento{" "}
              <span className="text-neutral-300 font-semibold">{documento}</span> ya precargado)
            </p>
            <button
              onClick={handleRetry}
              className="mt-6 text-sm text-neutral-500 hover:text-white underline underline-offset-2"
            >
              Volver al inicio
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
