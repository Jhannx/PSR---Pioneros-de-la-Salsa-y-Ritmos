import { useState } from "react";
import { Outlet, NavLink, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { ROLE_LABELS, ROLE_BADGE_STYLES } from "../context/mockDataConstants";
import {
  Users,
  UserCheck,
  Calendar,
  Clock,
  BarChart3,
  ShieldCheck,
  Settings,
  User,
  PhoneCall,
  LogOut,
  Menu,
  X,
  Tv,
  Monitor,
  ShieldQuestion,
} from "lucide-react";

export default function AppLayout() {
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const getNavItems = () => {
    if (!user || !user.roles) return [];

    const items = [];
    const roles = user.roles;

    // Los roles no son modos exclusivos entre los que se alterna — una persona puede ser
    // ADMIN y PROFESOR (o cualquier combinación) a la vez, con ambas autoridades en el mismo
    // token (ver PENDIENTES.md). Por eso cada `if` es independiente: se acumulan los accesos
    // de todos los roles que tenga, no solo el primero que coincida.
    if (roles.includes("ADMIN")) {
      items.push(
        { path: "/admin/reports", label: "Reportes - Dashboard", icon: BarChart3 },
        { path: "/reception/checkin", label: "Sesión - Aeróbicos", icon: UserCheck },
        { path: "/admin/people", label: "Personas registradas", icon: Users },
        { path: "/admin/sessions", label: "Sesiones de Clase", icon: Calendar },
        { path: "/admin/schedule", label: "Horarios Recurrentes", icon: Clock },
        { path: "/admin/roles", label: "Roles y Accesos", icon: ShieldQuestion },
        { path: "/admin/audit", label: "Auditoría de Actividad", icon: ShieldCheck },
        { path: "/admin/settings", label: "Configuración", icon: Settings }
      );
    }
    if (roles.includes("RECEPTIONIST")) {
      items.push(
        { path: "/reception/checkin", label: "Sesión - Aeróbicos", icon: UserCheck },
        { path: "/reception/people", label: "Personas registradas", icon: Users },
        { path: "/reception/sessions", label: "Sesiones de Clase", icon: Calendar },
        { path: "/reception/schedule", label: "Horarios Recurrentes", icon: Clock },
        // Único rol de staff (además de ADMIN) al que la matriz le da "Reportes de ingresos".
        { path: "/reception/reports", label: "Reportes - Dashboard", icon: BarChart3 },
        { path: "/reception/settings", label: "Configuración", icon: Settings }
      );
    }
    if (roles.includes("TEACHER")) {
      items.push(
        { path: "/reception/checkin", label: "Sesión - Aeróbicos", icon: UserCheck },
        { path: "/reception/people", label: "Personas registradas", icon: Users },
        { path: "/reception/sessions", label: "Sesiones de Clase", icon: Calendar },
        { path: "/reception/schedule", label: "Horarios Recurrentes", icon: Clock },
        // La matriz le da a PROFESOR "ver" tarifa/config de cola, pero no reportes de ingresos.
        { path: "/reception/settings", label: "Configuración", icon: Settings }
      );
    }
    if (roles.includes("MEMBER")) {
      items.push(
        { path: "/member/home", label: "Mi Estado & Asistencia", icon: UserCheck },
        { path: "/member/emergency-contact", label: "Contacto de Emergencia", icon: PhoneCall }
      );
    }
    if (roles.includes("PSR_TERMINAL")) {
      items.push(
        { path: "/lobby/checkin", label: "Ingreso por Documento", icon: Monitor },
        { path: "/lobby/register", label: "Registro Rápido", icon: User },
        { path: "/lobby/queue", label: "Fila Pública de Espera", icon: Tv }
      );
    }

    // "Mi Perfil" y el contacto de emergencia son de cualquier persona autenticada con
    // personId, sin importar el rol — un profesor o recepcionista también son personas.
    if (user.personId) {
      items.push({ path: "/member/emergency-contact", label: "Contacto de Emergencia", icon: PhoneCall });
    }
    items.push({ path: "/profile", label: "Mi Perfil", icon: User });

    const seen = new Set();
    return items.filter((item) => {
      if (seen.has(item.path)) return false;
      seen.add(item.path);
      return true;
    });
  };

  const navItems = getNavItems();

  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-100 flex flex-col md:flex-row font-body">
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,500;9..144,600;9..144,700&family=Manrope:wght@500;600;700;800&display=swap');
        .font-display { font-family: 'Fraunces', serif; font-optical-sizing: auto; }
        .font-body { font-family: 'Manrope', sans-serif; }
      `}</style>

      {/* Cabecera superior para dispositivos móviles */}
      <header className="md:hidden flex items-center justify-between px-4 py-3 bg-neutral-900 border-b border-neutral-800 shrink-0">
        <div className="flex items-center gap-2">
          <span className="font-bold text-lg text-white">PSR • Aeróbicos</span>
        </div>
        <button
          onClick={() => setMobileOpen((v) => !v)}
          className="p-2 text-neutral-400 hover:text-white rounded-lg bg-neutral-800"
        >
          {mobileOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </header>

      {/* Sidebar Fijo */}
      <aside
        className={`fixed md:sticky top-0 inset-y-0 left-0 z-40 w-60 h-screen bg-neutral-900 border-r border-neutral-800 flex flex-col shrink-0 transform transition-transform duration-200 ease-in-out ${
          mobileOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        }`}
      >
        {/* Logotipo */}
        <div className="px-4 py-3.5 border-b border-neutral-800 flex flex-col gap-0.5">
          <span className="text-lg font-bold tracking-tight text-white text-left">
            PSR • Aeróbicos
          </span>
          <p className="text-[11px] text-neutral-500 text-left">Panel de gestión</p>
        </div>

        {/* Elementos de navegación */}
        <nav className="flex-1 px-2.5 py-2.5 space-y-0.5 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;

            return (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={() => setMobileOpen(false)}
                className={`flex items-center justify-between px-2.5 py-2 rounded-xl text-xs font-semibold transition-all ${
                  isActive
                    ? "bg-emerald-500 text-neutral-950 font-bold shadow-md shadow-emerald-500/20"
                    : "text-neutral-300 hover:bg-neutral-800/80 hover:text-white"
                }`}
              >
                <div className="flex items-center gap-2.5">
                  <Icon size={16} className={isActive ? "text-neutral-950" : "text-neutral-400"} />
                  <span>{item.label}</span>
                </div>
              </NavLink>
            );
          })}
        </nav>

        {/* Pie del Sidebar: Usuario logueado */}
        <div className="p-3 border-t border-neutral-800 bg-neutral-900/50 flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5 overflow-hidden text-left">
              <div className="w-8 h-8 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center font-bold text-xs shrink-0">
                {user?.fullName ? user.fullName[0] : "US"}
              </div>
              <div className="overflow-hidden text-left">
                <p className="text-xs font-semibold text-white truncate text-left">{user?.fullName || "Usuario"}</p>
                <div className="flex flex-wrap gap-1 mt-0.5">
                  {user?.roles?.map((role) => (
                    <span
                      key={role}
                      className={`text-[9px] font-bold px-2 py-0.5 rounded-full border ${
                        ROLE_BADGE_STYLES[role] || "bg-neutral-800 text-neutral-300"
                      }`}
                    >
                      {ROLE_LABELS[role] || role}
                    </span>
                  ))}
                </div>
              </div>
            </div>
            <button
              onClick={handleLogout}
              title="Cerrar sesión"
              className="p-2 text-neutral-400 hover:text-red-400 hover:bg-neutral-800 rounded-xl transition-colors shrink-0"
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>

      {/* Área principal de contenido */}
      <main className="flex-1 overflow-x-hidden min-h-screen">
        <Outlet />
      </main>
    </div>
  );
}
