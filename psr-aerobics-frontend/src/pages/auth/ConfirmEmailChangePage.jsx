import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Mail, CheckCircle2, AlertCircle, LogIn } from "lucide-react";
import { peopleService } from "../../services/peopleService";

// Ruta pública — el enlace del correo apunta acá (EmailService.java, NOTAS-PARA-FRONTEND.md §2):
// {app.frontend.url}/confirmar-correo?token=... . Quien abre el enlace desde su buzón puede no
// estar logueado, así que esta pantalla no puede vivir detrás de ProtectedRoute. Si esta ruta no
// existiera con este path exacto, el enlace que ya está saliendo por correo caería en un 404.
export default function ConfirmEmailChangePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [status, setStatus] = useState(token ? "confirming" : "invalid"); // invalid | confirming | done | error
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    peopleService.confirmEmailChange(token)
      .then(() => {
        if (!cancelled) setStatus("done");
      })
      .catch((err) => {
        if (cancelled) return;
        // 400 típico: el enlace ya expiró (72h) o ya se usó. El backend manda mensaje legible.
        setErrorMessage(err.data?.message || err.message || "El enlace no es válido o ya expiró.");
        setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-center justify-center px-4 py-10 font-body">
      <div className="w-full max-w-md bg-neutral-900/60 border border-neutral-800 rounded-3xl p-6 text-center shadow-xl">
        {status === "invalid" && (
          <>
            <div className="w-14 h-14 rounded-2xl bg-red-500/15 border-2 border-red-500/50 flex items-center justify-center mx-auto mb-4 text-red-400">
              <AlertCircle size={24} />
            </div>
            <h1 className="text-lg font-bold text-white">Enlace inválido</h1>
            <p className="text-neutral-400 text-sm mt-1.5">
              Este enlace no incluye un token de confirmación. Revisa que copiaste la dirección completa desde
              el correo.
            </p>
          </>
        )}

        {status === "confirming" && (
          <>
            <div className="w-14 h-14 rounded-2xl bg-neutral-800 flex items-center justify-center mx-auto mb-4 text-neutral-400">
              <Mail size={24} />
            </div>
            <h1 className="text-lg font-bold text-white">Confirmando tu correo…</h1>
            <p className="text-neutral-400 text-sm mt-1.5">Un momento, estamos validando el enlace.</p>
          </>
        )}

        {status === "done" && (
          <>
            <div className="w-14 h-14 rounded-2xl bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-4">
              <CheckCircle2 className="text-emerald-400" size={24} />
            </div>
            <h1 className="text-lg font-bold text-white">Correo confirmado</h1>
            <p className="text-neutral-400 text-sm mt-1.5">
              Tu cuenta ya quedó asociada al correo nuevo. Puedes seguir usando el sistema con normalidad.
            </p>
            <button
              onClick={() => navigate("/login")}
              className="w-full mt-5 flex items-center justify-center gap-2 bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-3 rounded-xl transition-colors"
            >
              <LogIn size={18} /> Ir a iniciar sesión
            </button>
          </>
        )}

        {status === "error" && (
          <>
            <div className="w-14 h-14 rounded-2xl bg-red-500/15 border-2 border-red-500/50 flex items-center justify-center mx-auto mb-4 text-red-400">
              <AlertCircle size={24} />
            </div>
            <h1 className="text-lg font-bold text-white">No se pudo confirmar</h1>
            <p className="text-neutral-400 text-sm mt-1.5">{errorMessage}</p>
            <p className="text-neutral-600 text-xs mt-3">
              Puedes solicitar el cambio de correo de nuevo desde tu perfil.
            </p>
          </>
        )}
      </div>
    </div>
  );
}
