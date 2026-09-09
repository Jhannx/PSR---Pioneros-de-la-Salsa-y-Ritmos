import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { CreditCard, AlertCircle, RotateCcw, UserPlus, CheckCircle2, ArrowLeft } from "lucide-react";
import { formatTimeWithSeconds as formatTime } from "../../utils/date";
import { queueService } from "../../services/queueService";

export default function LobbyCheckinPage() {
  const navigate = useNavigate();

  const [step, setStep] = useState("documento"); // documento | no_encontrado | confirmado
  const [documento, setDocumento] = useState("");
  const [acceptedAt, setAcceptedAt] = useState(null);
  const [matchedPerson, setMatchedPerson] = useState(null);
  const [assignedPos, setAssignedPos] = useState(1);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");

  async function handleSubmit() {
    const trimmed = documento.trim();
    if (!trimmed || loading) return;

    setLoading(true);
    setErrorMsg("");

    try {
      // El backend resuelve la persona por número de documento y hace el check-in en una sola llamada.
      const checkinResult = await queueService.checkinByDocument(trimmed);
      setAcceptedAt(new Date());
      setMatchedPerson({ fullName: checkinResult?.fullName });
      setAssignedPos(checkinResult?.queuePosition || 1);
      setStep("confirmado");
    } catch (err) {
      console.error("Error al procesar check-in en la API:", err);
      // Si la API retorna 404 o no encuentra a la persona
      if (err.status === 404 || err.message?.toLowerCase().includes("no encontrad")) {
        setStep("no_encontrado");
      } else {
        setErrorMsg(err.message || "Error al conectar con el servidor");
      }
    } finally {
      setLoading(false);
    }
  }

  function handleRetry() {
    setDocumento("");
    setErrorMsg("");
    setStep("documento");
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex flex-col items-center justify-center px-6 py-10 font-body relative">
      <button
        onClick={() => navigate("/lobby/panel")}
        className="absolute top-6 left-6 flex items-center gap-2 text-neutral-400 hover:text-white text-xs bg-neutral-900 border border-neutral-800 px-3 py-2 rounded-xl transition-colors"
      >
        <ArrowLeft size={14} /> Volver al Panel
      </button>

      <div className="w-full max-w-lg">
        {step === "documento" && (
          <div className="text-center">
            <div className="w-16 h-16 rounded-2xl bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-6">
              <CreditCard className="text-emerald-400" size={28} />
            </div>
            <h1 className="text-2xl font-bold text-white mb-2">Ingresa tu documento</h1>
            <p className="text-neutral-400 text-sm mb-6">Para hacer check-in en la Terminal PSR de hoy</p>

            {errorMsg && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/30 text-rose-400 text-sm rounded-xl text-center">
                {errorMsg}
              </div>
            )}

            <input
              autoFocus
              value={documento}
              onChange={(e) => setDocumento(e.target.value.replace(/\D/g, ""))}
              onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
              placeholder="Ej. 1001234567"
              inputMode="numeric"
              className="w-full bg-neutral-900 border-2 border-neutral-800 rounded-2xl px-5 py-4 text-white text-2xl text-center tracking-widest placeholder-neutral-700 outline-none focus:border-emerald-500 transition-colors"
            />

            <div className="mt-6 flex flex-col gap-3">
              <button
                onClick={handleSubmit}
                disabled={!documento.trim() || loading}
                className="w-full bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold text-lg py-3.5 rounded-2xl transition-colors shadow-lg shadow-emerald-500/20"
              >
                {loading ? "Verificando..." : "Confirmar Asistencia"}
              </button>
            </div>
          </div>
        )}

        {step === "no_encontrado" && (
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-8 text-center">
            <div className="w-16 h-16 rounded-full bg-amber-500/15 border border-amber-500/30 flex items-center justify-center mx-auto mb-4 text-amber-400">
              <AlertCircle size={32} />
            </div>
            <h2 className="text-xl font-bold text-white mb-2">Documento no registrado</h2>
            <p className="text-neutral-400 text-sm mb-6">
              El documento <span className="text-white font-mono font-bold">{documento}</span> no aparece registrado en el sistema. Toda persona debe tener una cuenta para hacer check-in.
            </p>

            <div className="flex flex-col gap-3">
              <button
                onClick={() => navigate(`/lobby/register?documento=${encodeURIComponent(documento)}`)}
                className="w-full flex items-center justify-center gap-2 bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-3.5 rounded-xl transition-colors"
              >
                <UserPlus size={18} /> Crear cuenta en Terminal PSR
              </button>

              <button
                onClick={handleRetry}
                className="flex items-center justify-center gap-2 text-neutral-400 hover:text-white text-sm py-2 transition-colors"
              >
                <RotateCcw size={16} /> Intentar otro documento
              </button>
            </div>
          </div>
        )}

        {step === "confirmado" && (
          <div className="bg-neutral-900 border border-emerald-500/40 rounded-3xl p-8 text-center shadow-2xl">
            <div className="w-16 h-16 rounded-full bg-emerald-500/20 border border-emerald-500 flex items-center justify-center mx-auto mb-4 text-emerald-400">
              <CheckCircle2 size={36} />
            </div>
            <h2 className="text-3xl font-bold text-white mb-1 font-display">¡Check-in Registrado!</h2>
            <p className="text-emerald-400 font-semibold text-lg mb-6">
              {matchedPerson?.fullName || `Documento ${documento}`}
            </p>

            <div className="bg-neutral-950 border border-neutral-800 rounded-2xl p-6 mb-6">
              <span className="text-neutral-500 text-xs font-semibold uppercase tracking-wider block mb-1">
                Tu posición en la sala de espera
              </span>
              <span className="text-5xl font-extrabold text-white font-display">#{assignedPos}</span>
              <p className="text-neutral-400 text-xs mt-3">
                Hora de llegada registrada: {acceptedAt ? formatTime(acceptedAt) : ""}
              </p>
            </div>

            <button
              onClick={handleRetry}
              className="w-full bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold text-lg py-3.5 rounded-xl transition-colors"
            >
              Finalizar / Nuevo Check-in
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
