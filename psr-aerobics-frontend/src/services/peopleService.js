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
    // PersonRequest: { documentType, documentNumber, firstName, lastName, phone,
    // signedPhysicalConsent, healthObservations?, healthDataConsent?, emergencyContacts? }. El
    // alta manual de recepción no pasa por ningún checkbox digital — signedPhysicalConsent es la
    // confirmación de quien atiende el mostrador de que tiene el formato físico firmado delante,
    // no la aceptación de la persona registrada (NOTAS-PARA-FRONTEND.md §2). Sin él, 400. Los tres
    // últimos son opcionales (NOTAS-PARA-FRONTEND.md Bloque 2, C1) — salud y contactos de
    // emergencia, cargados de una vez con el resto del alta; si vienen, valen las mismas reglas
    // que en sus endpoints sueltos (máx. 2000 caracteres / máx. 5 contactos, consentimiento
    // obligatorio cuando hay contenido).
    return await apiClient.post("/api/personas", data);
  },

  autoRegister: async (data) => {
    // PersonRequest: { documentType, documentNumber, firstName, lastName, phone,
    // dataProcessingConsent, voluntaryParticipationConsent, healthObservations?,
    // healthDataConsent?, emergencyContacts? } — los dos primeros son los checkboxes de
    // RegistrationConsent.jsx (numerales 4.1 y 12); cualquiera en false/null/ausente, 400
    // (NOTAS-PARA-FRONTEND.md §1). Los tres últimos son opcionales (NOTAS-PARA-FRONTEND.md
    // Bloque 2, C1) — ver RegistrationHealthAndContacts.jsx.
    return await apiClient.post("/api/personas/autoregistro", data);
  },

  // Mostrador: reescribe documento, nombre y teléfono. Restringido a DESK — un MIEMBRO que llame
  // esto recibe 403, por eso "Mi Perfil" usa `updateMyData` de abajo, no este.
  update: async (id, data) => {
    // PersonUpdateRequest: { documentType, documentNumber, firstName, lastName, phone }
    return await apiClient.put(`/api/personas/${id}`, data);
  },

  // "Mi Perfil" (NOTAS-PARA-FRONTEND.md Bloque 2, A2) — endpoint propio para lo que sí es del
  // dueño: no lleva documento ni tipo de documento en el DTO, así que no hay forma de tocarlos por
  // acá, a propósito. DESK + el propio titular. Los cuatro campos son obligatorios (es un
  // reemplazo completo, como el PUT de mostrador).
  updateMyData: async (id, data) => {
    // { firstName, lastName, phone, birthDate }
    return await apiClient.put(`/api/personas/${id}/mis-datos`, data);
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
    // EmergencyContactRequest: { name, phone, thirdPartyConsent } — ya no lleva `observations`,
    // eso se mudó a observaciones de salud de la persona (ver updateHealthObservations más
    // abajo). Al menos uno de name/phone debe venir, o el backend responde 400. El sexto
    // contacto responde 409 — el frontend deshabilita "agregar" al llegar a 5 sin depender de
    // este error, pero se sigue manejando por si dos pestañas agregan a la vez.
    // `thirdPartyConsent` (numeral 5) se exige en TODA llamada, sin excepción — cada contacto es
    // una persona distinta, no hay "ya lo dio antes" que valga para el siguiente
    // (NOTAS-PARA-FRONTEND.md §4). Sin él, 400. Solo aplica al crear: `updateEmergencyContact`
    // no lo pide ni lo usa.
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
  //
  // `healthDataConsent` (numeral 4.2) es obligatorio en `true` cuando `healthObservations` no
  // está vacío — 400 si no viene. Al vaciar el campo se ignora, se puede mandar lo que sea
  // (NOTAS-PARA-FRONTEND.md §3): vaciar revoca el consentimiento, no lo exige.
  updateHealthObservations: async (personId, healthObservations, healthDataConsent) => {
    return await apiClient.put(`/api/personas/${personId}/observaciones-salud`, {
      healthObservations,
      healthDataConsent,
    });
  },

  // Estado y bitácora de consentimientos — numeral 10 (derecho a saber qué se autorizó y
  // cuándo). Permisos: personal, o la propia persona. `registrationConsentMissing` solo puede
  // ser `true` en cuentas de antes de este mecanismo; con la app recién desplegada no debería
  // aparecer nunca, pero el shape de la respuesta lo incluye igual (NOTAS-PARA-FRONTEND.md §GET).
  getConsents: async (personId) => {
    return await apiClient.get(`/api/personas/${personId}/consentimientos`);
  },

  deactivate: async (id) => {
    return await apiClient.patch(`/api/personas/${id}/desactivar`);
  },

  reactivate: async (id) => {
    return await apiClient.patch(`/api/personas/${id}/reactivar`);
  },

  remove: async (id) => apiClient.delete(`/api/personas/${id}`),
};
