// admin_checkin_desktop.jsx → capitalize
export function capitalize(text) {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

// admin_people_registry.jsx → getInitials(firstName, lastName)
export function getInitialsFromParts(firstName, lastName) {
  return `${firstName[0] || ""}${lastName[0] || ""}`.toUpperCase();
}

// vista_asistencia_movil.jsx → cálculo de iniciales inline, a partir de un solo string "Nombre Apellido"
export function getInitialsFromFullName(fullName) {
  return fullName
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}
