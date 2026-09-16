import { apiClient } from "./apiClient";

export const peopleService = {
  getAll: async (params = {}, options) => {
    const query = new URLSearchParams(params).toString();
    return await apiClient.get(`/api/personas${query ? `?${query}` : ""}`, options);
  },

  getById: async (id) => {
    return await apiClient.get(`/api/personas/${id}`);
  },

  getProfile: async (id) => {
    return await apiClient.get(`/api/personas/${id}/perfil`);
  },

  getBalance: async (id) => {
    return await apiClient.get(`/api/personas/${id}/saldo`);
  },

  create: async (data) => {
    // PersonRequest: { documentType, documentNumber, firstName, lastName, phone }
    return await apiClient.post("/api/personas", data);
  },

  autoRegister: async (data) => {
    // PersonRequest: { documentType, documentNumber, firstName, lastName, phone }
    return await apiClient.post("/api/personas/autoregistro", data);
  },

  update: async (id, data) => {
    // PersonUpdateRequest: { documentType, documentNumber, firstName, lastName, phone }
    return await apiClient.put(`/api/personas/${id}`, data);
  },

  // Cambio de correo en dos pasos — a propósito no es un PUT directo (NOTAS-PARA-FRONTEND.md §2):
  // si el correo se pudiera cambiar sin más, cualquiera con acceso momentáneo a una sesión ajena
  // podría apuntar la cuenta a un correo propio y adueñarse de ella desde "olvidé mi contraseña".
  // El enlace al buzón nuevo es la única prueba de que quien pide el cambio controla esa
  // dirección; ni un ADMIN lo salta.
  requestEmailChange: async (id, newEmail) => {
    // Autorizado: la propia persona, ADMIN o RECEPCIÓN. No cambia nada todavía — solo envía el
    // enlace de confirmación al correo nuevo. 409 si ese correo ya es de otra cuenta, 400 si es
    // el que ya tiene.
    return await apiClient.post(`/api/personas/${id}/solicitar-cambio-correo`, { newEmail });
  },

  confirmEmailChange: async (token, options) => {
    // Público — sin sesión: quien abre el enlace desde su buzón puede no estar logueado.
    // 400 si el enlace expiró (72h) o ya se usó. Acepta `options.signal` — ConfirmEmailChangePage
    // lo usa para cancelar de verdad la petición en vuelo si React StrictMode remonta el efecto en
    // dev, en vez de solo ignorar el resultado (errores-13sep-front.md #2).
    return await apiClient.post("/api/personas/confirmar-cambio-correo", { token }, options);
  },

  // Contactos de emergencia 1:N, máx. 5 por persona, todos con el mismo peso — sin orden de
  // prioridad, sin contacto "principal" (NOTAS-PARA-FRONTEND.md §6, CAMBIO ROMPEDOR: reemplaza
  // los antiguos GET/PUT /api/personas/{id}/contacto-emergencia, que eran 1:1 y ya no existen).
  //
  // Los permisos no son los mismos en los cuatro endpoints: listar/crear van sobre el `id` de la
  // PERSONA (personal, o la propia persona de ese id); editar/borrar van sobre el `id` del
  // CONTACTO (personal, o el dueño del contacto) — el backend resuelve de quién es antes de
  // dejar pasar, un miembro que intente tocar el contacto de otro recibe 403.
  getEmergencyContacts: async (personId) => {
    return await apiClient.get(`/api/personas/${personId}/contactos-emergencia`);
  },

  createEmergencyContact: async (personId, data) => {
    // EmergencyContactRequest: { name, phone } — ya no lleva `observations`, eso se mudó a
    // observaciones de salud de la persona (ver updateHealthObservations más abajo). Al menos
    // uno de los dos campos debe venir, o el backend responde 400. El sexto contacto responde
    // 409 — el frontend deshabilita "agregar" al llegar a 5 sin depender de este error, pero se
    // sigue manejando por si dos pestañas agregan a la vez.
    return await apiClient.post(`/api/personas/${personId}/contactos-emergencia`, data);
  },

  updateEmergencyContact: async (contactId, data) => {
    return await apiClient.put(`/api/contactos-emergencia/${contactId}`, data);
  },

  deleteEmergencyContact: async (contactId) => {
    return await apiClient.delete(`/api/contactos-emergencia/${contactId}`);
  },

  // Las observaciones de salud dejan de vivir en un contacto puntual y pasan a ser de la
  // persona, una sola vez (NOTAS-PARA-FRONTEND.md §6). Endpoint aparte de PUT /api/personas/{id}
  // a propósito: ese PUT reescribe documento/nombre/teléfono y está restringido a mostrador —
  // abrirlo al propio miembro para una nota de salud le dejaría también cambiar su documento.
  // `null` y cadena vacía son válidos (borrar lo escrito es legítimo), máx. 2000 caracteres.
  // Devuelve el PersonResponse completo, no solo el campo.
  updateHealthObservations: async (personId, healthObservations) => {
    return await apiClient.put(`/api/personas/${personId}/observaciones-salud`, { healthObservations });
  },

  deactivate: async (id) => {
    return await apiClient.patch(`/api/personas/${id}/desactivar`);
  },

  reactivate: async (id) => {
    return await apiClient.patch(`/api/personas/${id}/reactivar`);
  },

  remove: async (id) => apiClient.delete(`/api/personas/${id}`),
};
