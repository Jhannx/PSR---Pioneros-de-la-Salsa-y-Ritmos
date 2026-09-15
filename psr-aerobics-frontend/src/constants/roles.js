// Fuente única de todo lo relacionado con roles. Antes esta información estaba repartida en
// tres sitios que no se conocían entre sí: mockDataConstants.js (etiquetas/colores),
// AppLayout.jsx (qué ve cada rol, en una cadena de `if` acumulativos) y App.jsx (a dónde entra
// cada rol). El selector de rol (RoleContext, RoleSelectorPage) necesita las tres cosas a la
// vez, así que se centralizan acá. Ver docs/PLAN-IMPLEMENTACION.md §0.1.
import {
  Settings,
  Users,
  GraduationCap,
  User,
  UserCheck,
  Calendar,
  Clock,
  BarChart3,
  ShieldCheck,
  ShieldQuestion,
  PhoneCall,
  Monitor,
  Tv,
} from "lucide-react";

// Etiquetas de interfaz en español — movidas tal cual desde mockDataConstants.js.
export const ROLE_LABELS = {
  ADMIN: "Administrador",
  RECEPTIONIST: "Recepción",
  MEMBER: "Miembro",
  TEACHER: "Profesor",
  PSR_TERMINAL: "Terminal PSR",
};

// Colores de los badges en estilo pastilla redondeada — movidos tal cual desde
// mockDataConstants.js.
export const ROLE_BADGE_STYLES = {
  ADMIN: "bg-purple-500/10 border-purple-500/30 text-purple-400",
  RECEPTIONIST: "bg-emerald-500/10 border-emerald-500/30 text-emerald-400",
  MEMBER: "bg-blue-500/10 border-blue-500/30 text-blue-400",
  TEACHER: "bg-amber-500/10 border-amber-500/30 text-amber-400",
  PSR_TERMINAL: "bg-cyan-500/10 border-cyan-500/30 text-cyan-400",
};

// Metadata visual del selector de rol (docs/RoleSelectorPage.jsx): ícono y descripción corta de
// qué se puede hacer con cada rol. El mockup original usaba las claves en español
// (ADMIN/RECEPCIONISTA/PROFESOR/MIEMBRO); acá quedan traducidas a las claves reales que devuelve
// el backend (ADMIN/RECEPTIONIST/TEACHER/MEMBER) y se agrega PSR_TERMINAL, que el mockup no
// contemplaba porque es una cuenta técnica sin persona detrás.
export const ROLE_META = {
  ADMIN: { icon: Settings, label: "Administrador", description: "Control total del sistema" },
  RECEPTIONIST: { icon: Users, label: "Recepcionista", description: "Check-in, cobros y personas" },
  TEACHER: { icon: GraduationCap, label: "Profesor", description: "Tus sesiones y horarios" },
  MEMBER: { icon: User, label: "Miembro", description: "Tu asistencia y tu estado de cuenta" },
  PSR_TERMINAL: { icon: Monitor, label: "Terminal PSR", description: "Check-in y registro en recepción" },
};

// A dónde entra cada rol al loguear — extraído tal cual de RoleRedirect en App.jsx. El orden
// importa: si una cuenta tiene varios roles, se evalúa de arriba hacia abajo (por eso ADMIN va
// primero). Con el selector de rol (Fase 3) esto pasa a indexarse por rol activo en vez de
// recorrerse en cadena.
export const HOME_BY_ROLE = {
  ADMIN: "/admin/reports",
  RECEPTIONIST: "/reception/checkin",
  TEACHER: "/reception/checkin",
  PSR_TERMINAL: "/lobby/panel",
  MEMBER: "/member/home",
};

// Secciones de sidebar de cada rol — extraído tal cual de AppLayout.getNavItems(), sin cambiar
// ninguna ruta, etiqueta ni ícono. Antes se acumulaban con `if` independientes (una cuenta con
// dos roles veía la unión, sin orden); ahora AppLayout indexa por el rol activo.
export const NAV_BY_ROLE = {
  ADMIN: [
    { path: "/admin/reports", label: "Reportes - Dashboard", icon: BarChart3 },
    { path: "/reception/checkin", label: "Sesión - Aeróbicos", icon: UserCheck },
    { path: "/admin/people", label: "Personas registradas", icon: Users },
    { path: "/admin/sessions", label: "Sesiones de Clase", icon: Calendar },
    { path: "/admin/schedule", label: "Horarios Recurrentes", icon: Clock },
    { path: "/admin/roles", label: "Roles y Accesos", icon: ShieldQuestion },
    { path: "/admin/audit", label: "Auditoría de Actividad", icon: ShieldCheck },
    { path: "/admin/settings", label: "Configuración", icon: Settings },
  ],
  RECEPTIONIST: [
    { path: "/reception/checkin", label: "Sesión - Aeróbicos", icon: UserCheck },
    { path: "/reception/people", label: "Personas registradas", icon: Users },
    { path: "/reception/sessions", label: "Sesiones de Clase", icon: Calendar },
    { path: "/reception/schedule", label: "Horarios Recurrentes", icon: Clock },
    // Único rol de staff (además de ADMIN) al que la matriz le da "Reportes de ingresos".
    { path: "/reception/reports", label: "Reportes - Dashboard", icon: BarChart3 },
    { path: "/reception/settings", label: "Configuración", icon: Settings },
  ],
  TEACHER: [
    { path: "/reception/checkin", label: "Sesión - Aeróbicos", icon: UserCheck },
    { path: "/reception/people", label: "Personas registradas", icon: Users },
    { path: "/reception/sessions", label: "Sesiones de Clase", icon: Calendar },
    { path: "/reception/schedule", label: "Horarios Recurrentes", icon: Clock },
    // La matriz le da a PROFESOR "ver" tarifa/config de cola, pero no reportes de ingresos.
    { path: "/reception/settings", label: "Configuración", icon: Settings },
  ],
  MEMBER: [
    { path: "/member/home", label: "Mi Estado & Asistencia", icon: UserCheck },
    { path: "/emergency-contact", label: "Contacto de Emergencia", icon: PhoneCall },
  ],
  PSR_TERMINAL: [
    { path: "/lobby/checkin", label: "Ingreso por Documento", icon: Monitor },
    { path: "/lobby/register", label: "Registro Rápido", icon: User },
    { path: "/lobby/queue", label: "Fila Pública de Espera", icon: Tv },
  ],
};

// Todos los roles conocidos, en el orden en que se muestran en el selector.
export const ALL_ROLES = ["ADMIN", "RECEPTIONIST", "TEACHER", "MEMBER", "PSR_TERMINAL"];

// Rutas neutras: de cualquier persona autenticada con personId, sin importar el rol activo —
// aparecen en el sidebar de más de un rol (ver AppLayout.getNavItems) pero no "pertenecen" a
// ninguno. `/emergency-contact` está listada dentro de NAV_BY_ROLE.MEMBER (para que el sidebar de
// Miembro tenga el link), así que sin esta exclusión `getRolesForPath` la contaba como dueña de
// MEMBER — eso hacía que cualquier cuenta con otro rol activo (ej. Profesor) que entrara ahí se
// cambiara sola a Miembro sin aviso (errores-nuevo-front.md #4, síntoma 2 — bug real encontrado,
// no solo falta de aviso).
const NEUTRAL_PATHS = ["/profile", "/emergency-contact"];

// Qué roles "dueños" tiene una ruta, derivado de NAV_BY_ROLE — para el cambio automático de rol
// al entrar por URL directa (selector-rol-y-sesion.md §2, comportamiento 4). Devuelve [] para
// rutas neutras que no pertenecen a ningún rol en particular: esas no deben disparar un cambio de
// rol activo.
export function getRolesForPath(pathname) {
  if (NEUTRAL_PATHS.includes(pathname)) return [];
  return ALL_ROLES.filter((role) => (NAV_BY_ROLE[role] || []).some((item) => item.path === pathname));
}
