import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Mail, ArrowLeft, Send } from "lucide-react";
import { authService } from "../../services/authService";

const inputClass =
  "w-full bg-neutral-900 border border-neutral-800 rounded-xl px-4 py-3 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    if (!email.trim() || loading) return;
    setLoading(true);
    setErrorMessage("");
    try {
      // El backend responde 200 exista o no el correo, así que "sent" siempre
      // significa que la petición se procesó, no que la cuenta existe.
      await authService.forgotPassword(email.trim());
      setSent(true);
    } catch (err) {
      setErrorMessage(err.data?.message || err.message || "No fue posible procesar la solicitud.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-center justify-center px-4 py-10 font-body">
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Manrope:wght@500;600;700;800&display=swap');
        .font-body { font-family: 'Manrope', sans-serif; }
      `}</style>

      <div className="w-full max-w-md">
        <button
          onClick={() => navigate("/login")}
          className="flex items-center gap-2 text-neutral-500 hover:text-white text-sm mb-6 transition-colors"
        >
          <ArrowLeft size={16} /> Volver al inicio de sesión
        </button>

        {!sent ? (
          <form
            onSubmit={handleSubmit}
            className="bg-neutral-900/60 border border-neutral-800 rounded-3xl p-6 flex flex-col gap-4 shadow-xl"
          >
            <div>
              <h1 className="text-xl font-bold text-white">¿Olvidaste tu contraseña?</h1>
              <p className="text-neutral-400 text-sm mt-1.5">
                Escribe el correo de tu cuenta y te enviaremos un enlace para crear una nueva contraseña.
              </p>
            </div>

            {errorMessage && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl text-center">
                {errorMessage}
              </div>
            )}

            <div>
              <label className="text-neutral-300 text-sm font-semibold mb-1.5 block">Correo electrónico</label>
              <div className="relative">
                <Mail size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-neutral-500" />
                <input
                  autoFocus
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="correo@ejemplo.com"
                  className={`${inputClass} pl-11`}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={!email.trim() || loading}
              className="w-full flex items-center justify-center gap-2 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 active:scale-[0.98] transition-all text-neutral-950 font-bold py-3.5 rounded-2xl shadow-lg shadow-emerald-500/20"
            >
              <Send size={18} />
              {loading ? "Enviando..." : "Enviar enlace de recuperación"}
            </button>
          </form>
        ) : (
          <div className="bg-neutral-900/60 border border-neutral-800 rounded-3xl p-6 flex flex-col gap-4 shadow-xl text-center">
            <div className="w-14 h-14 rounded-2xl bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto">
              <Mail className="text-emerald-400" size={24} />
            </div>
            <div>
              <h1 className="text-lg font-bold text-white">Revisa tu correo</h1>
              <p className="text-neutral-400 text-sm mt-1.5">
                Si el correo existe en nuestro sistema, se envió un enlace de recuperación.
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
