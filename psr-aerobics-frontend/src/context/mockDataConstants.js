// Reexporta desde constants/roles.js, que pasó a ser la fuente única de todo lo relacionado con
// roles (ver docs/PLAN-IMPLEMENTACION.md §0.1). Este archivo se mantiene para no romper los
// imports existentes (AppLayout.jsx, ProtectedRoute.jsx, UserProfilePage.jsx) — los imports
// nuevos deberían apuntar directo a "../constants/roles".
export { ROLE_LABELS, ROLE_BADGE_STYLES } from "../constants/roles";
