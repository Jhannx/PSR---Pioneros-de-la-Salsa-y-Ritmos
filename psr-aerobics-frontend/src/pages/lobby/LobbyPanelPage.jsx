import { useNavigate } from "react-router-dom";
import { Monitor, UserPlus, Tv, QrCode, ArrowRight } from "lucide-react";

const PANEL_OPTIONS = [
  {
    path: "/lobby/checkin",
    label: "Ingreso por Documento",
    description: "Pantalla de check-in donde la persona digita su documento para hacer fila.",
    icon: Monitor,
  },
  {
    path: "/lobby/qr",
    label: "Ingreso por QR",
    description: "Código QR para que la persona marque su asistencia desde su propio celular.",
    icon: QrCode,
  },
  {
    path: "/lobby/register",
    label: "Registro Rápido",
    description: "Crear una cuenta nueva cuando el documento no aparece registrado.",
    icon: UserPlus,
  },
  {
    path: "/lobby/queue",
    label: "Fila Pública de Espera",
    description: "Pantalla de TV con los llamados, prepagados y la fila en espera.",
    icon: Tv,
  },
];

export default function LobbyPanelPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-4 md:px-6 py-4 font-body text-white">
      <div className="max-w-5xl mx-auto space-y-4">
        <div className="border-b border-neutral-800 pb-3">
          <h1 className="text-xl font-bold text-white tracking-tight">Panel de la Terminal PSR</h1>
          <p className="text-neutral-400 text-xs mt-0.5">
            Elige la pantalla que quieres mostrar en este equipo. Cada una se abre a pantalla completa.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {PANEL_OPTIONS.map((option) => {
            const Icon = option.icon;
            return (
              <button
                key={option.path}
                onClick={() => navigate(option.path)}
                className="text-left bg-neutral-900 border border-neutral-800 hover:border-emerald-500/50 rounded-3xl p-6 flex flex-col gap-4 transition-colors group"
              >
                <div className="w-12 h-12 rounded-2xl bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
                  <Icon size={22} />
                </div>
                <div className="flex-1">
                  <h2 className="text-white font-bold text-base mb-1">{option.label}</h2>
                  <p className="text-neutral-400 text-xs">{option.description}</p>
                </div>
                <span className="flex items-center gap-1.5 text-emerald-400 text-xs font-semibold">
                  Abrir pantalla completa
                  <ArrowRight size={14} className="transition-transform group-hover:translate-x-0.5" />
                </span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
