import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, X } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { useRole } from "../../context/RoleContext";
import { ROLE_META, HOME_BY_ROLE } from "../../constants/roles";

// Integración del mockup de docs/RoleSelectorPage.jsx (docs/selector-rol-y-sesion.md,
// docs/PLAN-IMPLEMENTACION.md Fase 3). El diseño visual se conserva íntegro; lo único que
// cambia es lo que el propio mockup marcaba como provisional en sus comentarios: `mockRoles` y
// `ROLE_META` hardcodeados pasan a salir de RoleContext y constants/roles.js.
//
// El modo se deriva solo de si ya hay un rol activo — no de cómo se llegó a la ruta:
// `activeRole` nulo (recién logueado, más de un rol, sin elección previa) → modo "initial", sin
// botón de cancelar. `activeRole` ya definido (se entra desde el badge del sidebar) → modo
// "switch", con X para volver sin cambiar nada.
export default function RoleSelectorPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { activeRole, availableRoles, setActiveRole } = useRole();
  const [selected, setSelected] = useState(null);

  const mode = activeRole ? "switch" : "initial";

  function handleSelectRole(role) {
    setActiveRole(role);
    navigate(HOME_BY_ROLE[role] || "/", { replace: true });
  }

  function handleConfirm() {
    if (!selected) return;
    handleSelectRole(selected);
  }

  function handleCancel() {
    navigate(HOME_BY_ROLE[activeRole] || "/", { replace: true });
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 relative overflow-hidden flex items-center justify-center px-4 py-10">
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,500;9..144,600;9..144,700&family=Manrope:wght@500;600;700;800&display=swap');
        .font-display { font-family: 'Fraunces', serif; font-optical-sizing: auto; }
        .font-body { font-family: 'Manrope', sans-serif; }
      `}</style>

      <div
        aria-hidden="true"
        className="pointer-events-none absolute -top-40 left-1/2 -translate-x-1/2 w-[560px] h-[560px] rounded-full bg-emerald-500/10 blur-3xl"
      />

      <div className="font-body w-full max-w-md relative">
        {mode === "switch" && (
          <button
            onClick={handleCancel}
            className="absolute -top-2 right-0 text-neutral-500 hover:text-white p-2 transition-colors"
          >
            <X size={20} />
          </button>
        )}

        <div className="text-center mb-8">
          <p className="font-display text-3xl text-white tracking-tight">
            {mode === "initial" ? `Hola, ${user?.fullName || "de nuevo"}` : "Cambiar de rol"}
          </p>
          <p className="text-neutral-500 text-sm mt-2">
            {mode === "initial"
              ? "Tienes más de un rol asignado — ¿con cuál entras hoy?"
              : "Elige con qué rol quieres seguir navegando"}
          </p>
        </div>

        <div className="flex flex-col gap-3 mb-6">
          {availableRoles.map((role) => {
            const meta = ROLE_META[role];
            if (!meta) return null;
            const Icon = meta.icon;
            const isSelected = selected === role;
            const isCurrent = mode === "switch" && role === activeRole;

            return (
              <button
                key={role}
                onClick={() => setSelected(role)}
                className={`flex items-center gap-4 px-5 py-4 rounded-2xl border-2 text-left transition-colors ${
                  isSelected
                    ? "bg-emerald-500/10 border-emerald-500"
                    : "bg-neutral-900 border-neutral-800 hover:border-neutral-700"
                }`}
              >
                <div
                  className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${
                    isSelected ? "bg-emerald-500 text-neutral-950" : "bg-neutral-800 text-neutral-400"
                  }`}
                >
                  <Icon size={20} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className={`font-bold ${isSelected ? "text-white" : "text-neutral-200"}`}>{meta.label}</p>
                    {isCurrent && (
                      <span className="text-[10px] font-semibold uppercase tracking-wide text-neutral-500 bg-neutral-800 px-2 py-0.5 rounded-full">
                        Activo
                      </span>
                    )}
                  </div>
                  <p className="text-neutral-500 text-sm">{meta.description}</p>
                </div>
              </button>
            );
          })}
        </div>

        <button
          onClick={handleConfirm}
          disabled={!selected}
          className="w-full flex items-center justify-center gap-2 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 active:scale-[0.98] transition-all text-neutral-950 font-bold text-lg py-4 rounded-2xl shadow-lg shadow-emerald-500/20"
        >
          Entrar
          <ArrowRight size={20} />
        </button>
      </div>
    </div>
  );
}
