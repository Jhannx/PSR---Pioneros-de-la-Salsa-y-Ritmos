/* eslint-disable react-refresh/only-export-components */
// Decide qué rol está "activo" cuando una cuenta tiene más de uno — puramente de presentación
// (qué se muestra), nunca de autorización (qué se permite). ProtectedRoute sigue chequeando
// contra `user.roles` completo, exactamente igual que antes de que existiera este contexto: si
// alguien manipula el rol activo desde la consola, lo único que logra es un sidebar raro, el
// backend rechaza igual cualquier acción que ese rol no tenga permitida de verdad.
// Ver docs/selector-rol-y-sesion.md §2-3 y docs/PLAN-IMPLEMENTACION.md Fase 3.
import { createContext, useContext, useEffect, useRef, useState } from "react";
import { useAuth } from "./AuthContext";

const RoleContext = createContext(null);

// Guardado ligado a la persona, no al dispositivo — un mapa { [userId]: role } en vez de un
// solo valor global. Así dos personas que comparten el computador de recepción no heredan el
// rol activo de la otra (selector-rol-y-sesion.md §2).
const ROLE_STORAGE_KEY = "psr_active_role_by_user";

function readStoredRoles() {
  try {
    const raw = localStorage.getItem(ROLE_STORAGE_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

// Guarda, junto al rol elegido, qué roles tenía la cuenta la última vez que se guardó — así se
// distingue "el rol recordado sigue siendo válido" de "a la cuenta le agregaron un rol nuevo
// desde entonces". Sin esto, a alguien que entraba como MIEMBRO y le asignaban PROFESOR le
// seguía entrando a MIEMBRO directo, sin mostrarle nunca el selector (errores-nuevo-front.md
// #4, síntoma 1).
const KNOWN_ROLES_STORAGE_KEY = "psr_known_roles_by_user";

function readKnownRoles() {
  try {
    const raw = localStorage.getItem(KNOWN_ROLES_STORAGE_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

function writeKnownRoles(userId, roles) {
  const known = readKnownRoles();
  known[userId] = roles;
  try {
    localStorage.setItem(KNOWN_ROLES_STORAGE_KEY, JSON.stringify(known));
  } catch (e) {
    console.error("No se pudo guardar el conjunto de roles conocido:", e);
  }
}

function computeInitialRole(user) {
  if (!user) return null;
  const roles = user.roles || [];
  const stored = readStoredRoles()[user.userId];
  const previouslyKnown = readKnownRoles()[user.userId] || [];
  const hasNewRole = roles.some((role) => !previouslyKnown.includes(role));
  writeKnownRoles(user.userId, roles);
  // Si el rol recordado ya no está entre los roles reales (se lo quitaron), se descarta — no se
  // arrastra un rol fantasma. Si le agregaron un rol nuevo desde la última vez, también se
  // descarta el recordado a propósito: la persona debe volver a elegir con la opción nueva a la
  // vista, no quedar encerrada en el rol de antes.
  if (stored && roles.includes(stored) && !hasNewRole) return stored;
  // Un solo rol → entra directo, sin fricción. Más de uno y sin rol recordado válido → queda en
  // null, que es la señal de "hace falta el selector".
  if (roles.length === 1) return roles[0];
  return null;
}

// Usado desde `LoginPage` para decidir la redirección justo tras loguear, antes de que este
// contexto tenga oportunidad de re-renderizar con el `user` nuevo (errores-12sep-front.md #6):
// `LoginPage` navegaba directo a `state.from` (la ruta que expulsó al login) sin pasar nunca por
// `needsRoleSelection`, así que una cuenta multirol volvía a la última URL protegida que visitó
// en vez de al selector. Misma lógica que `computeInitialRole`, expuesta aparte para no duplicarla.
export function needsRoleSelectionFor(user) {
  const roles = user?.roles || [];
  return roles.length > 1 && !computeInitialRole(user);
}

export function RoleProvider({ children }) {
  const { user } = useAuth();
  const [activeRole, setActiveRoleState] = useState(() => computeInitialRole(user));
  const lastUserIdRef = useRef(user?.userId ?? null);

  useEffect(() => {
    // Solo se recalcula si cambió la cuenta autenticada (login, logout, o cambio de cuenta). Un
    // refresco de `user` que no cambia de cuenta (ej. updateUserProfile tras editar el nombre)
    // no debe pisar un cambio de rol que la persona ya haya hecho en esta sesión.
    if ((user?.userId ?? null) === lastUserIdRef.current) return;
    lastUserIdRef.current = user?.userId ?? null;
    setActiveRoleState(computeInitialRole(user));
  }, [user]);

  const availableRoles = user?.roles || [];

  function setActiveRole(role) {
    if (!user || !availableRoles.includes(role)) return; // rechaza en silencio un rol que no tiene
    setActiveRoleState(role);
    const stored = readStoredRoles();
    stored[user.userId] = role;
    try {
      localStorage.setItem(ROLE_STORAGE_KEY, JSON.stringify(stored));
    } catch (e) {
      console.error("No se pudo guardar el rol activo:", e);
    }
  }

  const needsRoleSelection = availableRoles.length > 1 && !activeRole;

  return (
    <RoleContext.Provider value={{ activeRole, availableRoles, setActiveRole, needsRoleSelection }}>
      {children}
    </RoleContext.Provider>
  );
}

export function useRole() {
  const context = useContext(RoleContext);
  if (!context) {
    throw new Error("useRole debe ser usado dentro de un RoleProvider");
  }
  return context;
}
