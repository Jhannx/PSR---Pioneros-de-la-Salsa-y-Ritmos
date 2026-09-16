// Validaciones mínimas de formulario, front-only (errores-13sep-front.md #3, pedido explícito:
// "asi sea front only, un num de telefono minimo de caracteres, minimo 10, con num de doc minimo
// 6"). No reemplazan ninguna validación real del backend — solo dan feedback inmediato antes de
// mandar la petición, para no depender de un 400 para enterarse de un dato obviamente incompleto.

export const MIN_PHONE_DIGITS = 10;
export const MIN_DOCUMENT_DIGITS = 6;

/** Cuenta solo dígitos — ignora espacios, guiones, paréntesis, etc. que la persona haya tecleado. */
export function digitsOnly(value) {
  return String(value || "").replace(/\D/g, "");
}

export function isValidPhone(value) {
  return digitsOnly(value).length >= MIN_PHONE_DIGITS;
}

export function isValidDocumentNumber(value) {
  return digitsOnly(value).length >= MIN_DOCUMENT_DIGITS;
}

// Regla real del backend para el teléfono de contacto de emergencia
// (NOTAS-PARA-FRONTEND.md ronda 5, §1.3): `^\+?[0-9 ]{7,20}$` — dígitos y espacios, prefijo
// internacional opcional, 7 a 20 caracteres EN TOTAL (no solo dígitos). A propósito NO se reusa
// `isValidPhone`/`MIN_PHONE_DIGITS` (10 dígitos) acá: el backend advierte explícitamente que una
// validación más estricta que la suya bloquearía números que sí acepta (ej. un fijo de 7 dígitos).
export const EMERGENCY_CONTACT_PHONE_PATTERN = /^\+?[0-9 ]{7,20}$/;

export function isValidEmergencyContactPhone(value) {
  return EMERGENCY_CONTACT_PHONE_PATTERN.test(String(value || "").trim());
}
