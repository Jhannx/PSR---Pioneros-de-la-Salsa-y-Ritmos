import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { LogIn, Eye, EyeOff } from "lucide-react";
import { useAuth } from "../../context/AuthContext";

const ACADEMY_NAME = "PSR Aeróbicos";

const inputClass =
  "w-full bg-neutral-900 border border-neutral-800 rounded-xl px-4 py-3 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const canSubmit = identifier.trim() && password.trim() && !loading;

  async function handleSubmit(e) {
    e.preventDefault();
    if (!canSubmit) return;
    setError(null);
    setLoading(true);

    try {
      const result = await login(identifier, password);
      setLoading(false);
      if (result.success) {
        navigate("/");
      } else {
        setError(result.error || "Documento/correo o contraseña incorrectos");
      }
    } catch (err) {
      setLoading(false);
      setError(err.message || "Error de conexión al servidor");
    }
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 relative overflow-hidden flex items-center justify-center px-4 py-10 font-body">
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,500;9..144,600;9..144,700&family=Manrope:wght@500;600;700;800&display=swap');
        .font-display { font-family: 'Fraunces', serif; font-optical-sizing: auto; }
        .font-body { font-family: 'Manrope', sans-serif; }
      `}</style>

      {/* Resplandor de marca */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -top-40 left-1/2 -translate-x-1/2 w-[560px] h-[560px] rounded-full bg-emerald-500/10 blur-3xl"
      />

      <div className="w-full max-w-md relative">
        <div className="text-center mb-8">
          <p className="font-display text-4xl text-white tracking-tight">{ACADEMY_NAME}</p>
          <p className="text-neutral-500 text-sm mt-2">Ingresa a tu cuenta para continuar</p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="bg-neutral-900/60 border border-neutral-800 rounded-3xl p-6 flex flex-col gap-4 shadow-xl"
        >
          <div>
            <label className="text-neutral-300 text-sm font-semibold mb-1.5 block">
              Documento o correo electrónico
            </label>
            <input
              autoFocus
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              placeholder="1001234567 / laura.gomez@gmail.com"
              className={inputClass}
            />
          </div>

          <div>
            <label className="text-neutral-300 text-sm font-semibold mb-1.5 block">Contraseña</label>
            <div className="relative">
              <input
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className={`${inputClass} pr-11`}
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

          {error && (
            <div className="bg-red-500/10 border border-red-500/30 text-red-400 text-sm rounded-xl px-3.5 py-2.5">
              {error}
            </div>
          )}

          <button
            type="button"
            onClick={() => navigate("/forgot-password")}
            className="text-neutral-500 hover:text-white text-sm text-right -mt-1 transition-colors"
          >
            ¿Olvidaste tu contraseña?
          </button>

          <button
            type="submit"
            disabled={!canSubmit}
            className="w-full flex items-center justify-center gap-2 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 active:scale-[0.98] transition-all text-neutral-950 font-bold text-lg py-3.5 rounded-2xl shadow-lg shadow-emerald-500/20"
          >
            <LogIn size={20} />
            {loading ? "Ingresando..." : "Ingresar"}
          </button>
        </form>

        <p className="text-center text-neutral-600 text-sm mt-6">
          ¿Primera vez en {ACADEMY_NAME}?{" "}
          <button
            onClick={() => navigate("/register")}
            className="text-emerald-400 hover:text-emerald-300 font-semibold transition-colors"
          >
            Regístrate
          </button>
        </p>
      </div>
    </div>
  );
}
