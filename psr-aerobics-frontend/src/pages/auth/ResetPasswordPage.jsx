import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { KeyRound, Eye, EyeOff, ArrowLeft, CheckCircle2, AlertCircle } from "lucide-react";
import { authService } from "../../services/authService";

const inputClass =
  "w-full bg-neutral-900 border border-neutral-800 rounded-xl px-4 py-3 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const canSubmit = newPassword.length >= 8 && newPassword === confirmPassword && !loading;

  async function handleSubmit(e) {
    e.preventDefault();
    if (!canSubmit) return;
    setLoading(true);
    setErrorMessage("");
    try {
      await authService.resetPassword(token, newPassword);
      setDone(true);
    } catch (err) {
      setErrorMessage(err.data?.message || err.message || "No fue posible restablecer la contraseña.");
    } finally {
      setLoading(false);
    }
  }

  if (!token) {
    return (
      <div className="min-h-screen w-full bg-neutral-950 flex items-center justify-center px-4 py-10 font-body">
        <div className="w-full max-w-md bg-neutral-900/60 border border-neutral-800 rounded-3xl p-6 text-center shadow-xl">
          <div className="w-14 h-14 rounded-2xl bg-red-500/15 border-2 border-red-500/50 flex items-center justify-center mx-auto mb-4 text-red-400">
            <AlertCircle size={24} />
          </div>
          <h1 className="text-lg font-bold text-white">Enlace inválido</h1>
          <p className="text-neutral-400 text-sm mt-1.5">
            Este enlace no incluye un token de recuperación. Solicita uno nuevo desde "¿Olvidaste tu contraseña?".
          </p>
          <button
            onClick={() => navigate("/forgot-password")}
            className="w-full mt-5 bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-3 rounded-xl transition-colors"
          >
            Solicitar nuevo enlace
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-center justify-center px-4 py-10 font-body">
      <div className="w-full max-w-md">
        {!done && (
          <button
            onClick={() => navigate("/login")}
            className="flex items-center gap-2 text-neutral-500 hover:text-white text-sm mb-6 transition-colors"
          >
            <ArrowLeft size={16} /> Volver al inicio de sesión
          </button>
        )}

        {done ? (
          <div className="bg-neutral-900/60 border border-neutral-800 rounded-3xl p-6 flex flex-col gap-4 shadow-xl text-center">
            <div className="w-14 h-14 rounded-2xl bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto">
              <CheckCircle2 className="text-emerald-400" size={24} />
            </div>
            <div>
              <h1 className="text-lg font-bold text-white">Contraseña actualizada correctamente</h1>
              <p className="text-neutral-400 text-sm mt-1.5">Ya puedes ingresar con tu nueva contraseña.</p>
            </div>
            <button
              onClick={() => navigate("/login")}
              className="w-full bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-3 rounded-xl transition-colors"
            >
              Ir a iniciar sesión
            </button>
          </div>
        ) : (
          <form
            onSubmit={handleSubmit}
            className="bg-neutral-900/60 border border-neutral-800 rounded-3xl p-6 flex flex-col gap-4 shadow-xl"
          >
            <div>
              <h1 className="text-xl font-bold text-white">Crea tu nueva contraseña</h1>
              <p className="text-neutral-400 text-sm mt-1.5">Debe tener entre 8 y 100 caracteres.</p>
            </div>

            {errorMessage && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl text-center">
                {errorMessage}
              </div>
            )}

            <div>
              <label className="text-neutral-300 text-sm font-semibold mb-1.5 block">Nueva contraseña</label>
              <div className="relative">
                <KeyRound size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-neutral-500" />
                <input
                  autoFocus
                  type={showPassword ? "text" : "password"}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••"
                  className={`${inputClass} pl-11 pr-11`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-500 hover:text-white transition-colors"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <div>
              <label className="text-neutral-300 text-sm font-semibold mb-1.5 block">Confirmar contraseña</label>
              <input
                type={showPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="••••••••"
                className={inputClass}
              />
              {confirmPassword && newPassword !== confirmPassword && (
                <p className="text-red-400 text-xs mt-1.5">Las contraseñas no coinciden.</p>
              )}
            </div>

            <button
              type="submit"
              disabled={!canSubmit}
              className="w-full flex items-center justify-center gap-2 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 active:scale-[0.98] transition-all text-neutral-950 font-bold py-3.5 rounded-2xl shadow-lg shadow-emerald-500/20"
            >
              {loading ? "Guardando..." : "Guardar nueva contraseña"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
