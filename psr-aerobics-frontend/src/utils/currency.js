// Formato de moneda colombiana — usado en cualquier pantalla que muestre
// dinero (recepción, reportes, configuración de tarifa).
export function formatCOP(value) {
  return `$${Number(value || 0).toLocaleString("es-CO")}`;
}
