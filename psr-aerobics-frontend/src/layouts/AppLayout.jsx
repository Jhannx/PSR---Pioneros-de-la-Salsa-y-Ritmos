import { useEffect, useRef, useState } from "react";
import { Outlet, NavLink, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useRole } from "../context/RoleContext";
import { ROLE_LABELS, ROLE_BADGE_STYLES, NAV_BY_ROLE, getRolesForPath } from "../constants/roles";
import { User, PhoneCall, LogOut, Menu, X, ChevronsUpDown } from "lucide-react";

export default function AppLayout() {
  const { user, logout } = useAuth();
  const { activeRole, availableRoles, setActiveRole } = useRole();
  const [mobileOpen, setMobileOpen] = useState(false);
  // Aviso breve cuando el efecto de abajo cambia el rol activo sola — antes pasaba sin ningún
  // feedback, lo que generaba confusión sobre por qué el sidebar cambió de repente
  // (errores-nuevo-front.md #4, síntoma 2).
  const [roleSwitchNotice, setRoleSwitchNotice] = useState(null);
  const autoSwitchedRef = useRef(false);
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  // Cambio automático de rol al entrar por URL directa (selector-rol-y-sesion.md §2,
  // comportamiento 4): si la ruta actual pertenece a un rol que la cuenta tiene pero que no
  // está activo ahora mismo, el rol activo cambia solo — si no, el sidebar quedaría mostrando
  // las secciones de un rol y el contenido las de otro. `getRolesForPath` devuelve [] para
  // rutas neutras (/profile, /emergency-contact), que no pertenecen a ningún rol en particular:
  // esas nunca disparan un cambio. Si el rol activo ya es dueño de la ruta, tampoco se toca
  // nada — varias rutas (ej. /reception/checkin) son de más de un rol a la vez.
  useEffect(() => {
    const owners = getRolesForPath(location.pathname);
    if (owners.length === 0 || owners.includes(activeRole)) return;
    const fallback = owners.find((role) => availableRoles.includes(role));
    if (fallback) {
      autoSwitchedRef.current = true;
      setActiveRole(fallback);
    }
  }, [location.pathname, activeRole, availableRoles, setActiveRole]);

  // Separado del efecto de arriba (que ya tiene su propio setState) para que cada efecto dispare
  // como mucho una actualización de estado — dos en el mismo generarían un render en cascada.
  useEffect(() => {
    if (!autoSwitchedRef.current) return;
    autoSwitchedRef.current = false;
    setRoleSwitchNotice(activeRole);
  }, [activeRole]);

  useEffect(() => {
    if (!roleSwitchNotice) return;
    const timer = setTimeout(() => setRoleSwitchNotice(null), 4000);
    return () => clearTimeout(timer);
  }, [roleSwitchNotice]);

  // Un solo rol activo a la vez — antes se acumulaban las secciones de TODOS los roles con `if`
  // independientes, así que una cuenta con dos roles veía la unión sin ningún orden
  // (errores-frontend.md #8). Ahora el sidebar pinta solo NAV_BY_ROLE[activeRole].
  function getNavItems() {
    if (!user) return [];
    const items = [...(NAV_BY_ROLE[activeRole] || [])];

    // "Mi Perfil" y el contacto de emergencia son de cualquier persona autenticada con
    // personId, sin importar el rol activo — un profesor o recepcionista también son personas.
    if (user.personId) {
      items.push({ path: "/emergency-contact", label: "Contacto de Emergencia", icon: PhoneCall });
    }
    items.push({ path: "/profile", label: "Mi Perfil", icon: User });

    const seen = new Set();
    return items.filter((item) => {
      if (seen.has(item.path)) return false;
      seen.add(item.path);
      return true;
    });
  }

  const navItems = getNavItems();
  const canSwitchRole = availableRoles.length > 1;

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
                {/* Solo el badge del rol activo — antes se pintaban todos los roles a la vez,
                    que es justamente la señal visual del sidebar mezclado (errores-frontend.md
                    #8). Si hay más de un rol, el badge es un botón que abre el selector en modo
                    "switch" sin pasar por logout (selector-rol-y-sesion.md §2). */}
                {activeRole && (
                  canSwitchRole ? (
                    <button
                      onClick={() => navigate("/seleccionar-rol")}
                      title="Cambiar de rol"
                      className={`mt-0.5 inline-flex items-center gap-1 text-[9px] font-bold px-2 py-0.5 rounded-full border transition-opacity hover:opacity-80 ${
                        ROLE_BADGE_STYLES[activeRole] || "bg-neutral-800 text-neutral-300"
                      }`}
                    >
                      {ROLE_LABELS[activeRole] || activeRole}
                      <ChevronsUpDown size={9} />
                    </button>
                  ) : (
                    <span
                      className={`mt-0.5 inline-flex text-[9px] font-bold px-2 py-0.5 rounded-full border ${
                        ROLE_BADGE_STYLES[activeRole] || "bg-neutral-800 text-neutral-300"
                      }`}
                    >
                      {ROLE_LABELS[activeRole] || activeRole}
                    </span>
                  )
                )}
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

      {roleSwitchNotice && (
        <div className="fixed bottom-4 right-4 z-50 bg-neutral-900 border border-emerald-500/40 text-white text-xs font-semibold px-4 py-2.5 rounded-xl shadow-xl shadow-black/40 flex items-center gap-2">
          <ChevronsUpDown size={14} className="text-emerald-400 shrink-0" />
          Cambiaste a modo {ROLE_LABELS[roleSwitchNotice] || roleSwitchNotice}
        </div>
      )}
    </div>
  );
}
