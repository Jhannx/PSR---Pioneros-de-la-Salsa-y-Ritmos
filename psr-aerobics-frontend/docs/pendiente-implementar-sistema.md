# Pendiente implementar en el sistema — Política de Datos

No es el documento legal — es lo que hay que construir o decidir para
que la política sea cierta. Se actualiza a medida que se resuelve cada
punto.

---

## Implementación técnica pendiente

Nada de esto existe hoy en el sistema real — ni el texto ya redactado
para la interfaz, que todavía no está puesto en ninguna pantalla.

### Frontend — ya implementado (2026-09-16)

Los 6 puntos de abajo ya tienen código; se dejan aquí en vez de
borrarlos porque cada uno aclara una decisión de alcance que no era
obvia solo leyendo el pedido original.

1. **Página pública de la política**: `/politica-privacidad`
   (`pages/public/PrivacyPolicyPage.jsx`), transcripción fiel de
   `politica-tratamiento-datos.md`. `[FECHA]` y `[FECHA DE
   PUBLICACIÓN]` se dejaron literales a propósito — ver "Ya resuelto".
2. **Checkbox de aceptación general + participación voluntaria**
   (`components/shared/RegistrationConsent.jsx`, dos casillas
   separadas, política numeral 12): **decisión de alcance** — "el que
   hace recepción" se interpretó como `LobbyRegistrationPage.jsx` (el
   autoregistro que corre en la terminal de recepción), no como
   `PersonFormModal.jsx` (el alta manual que hace recepción/admin
   tecleando por la persona). La razón: `PersonFormModal` usa un
   formato físico firmado en vez del checkbox digital (ver "Decisiones
   de negocio pendientes" #2 más abajo, y el comentario de alcance que
   ya traía `RegistrationConfirmModal.jsx`: "No aplica a
   PersonFormModal... ahí ya hay una persona supervisando el dato").
   Así, el checkbox vive en `SelfRegistrationPage.jsx` y
   `LobbyRegistrationPage.jsx` — los dos flujos de **autoregistro
   digital** — y bloquea el envío si falta cualquiera de las dos.
3. **Checkbox independiente de datos de salud** (política numeral
   4.2): en `MemberEmergencyContactPage.jsx`, junto a "Observaciones de
   salud" (es el único lugar donde se captura ese dato — no se pide en
   el registro). Solo se exige si hay contenido nuevo por guardar;
   vaciar el campo nunca lo requiere, tal como dice el numeral 4.2.
4. **Texto de autorización de terceros** (política numeral 5): en
   `MemberEmergencyContactPage.jsx`, junto al formulario de "agregar
   contacto". Es texto informativo, no checkbox — la política solo
   pide "mostrarlo", no pedir una casilla aparte.
5. **Ver/descargar los propios datos**: sección "Mis Datos" en
   `UserProfilePage.jsx` — un botón que ensambla perfil + contactos de
   emergencia + saldo (los tres que la política numeral 6 ya autoriza
   para "la propia persona titular") y descarga un `.json`. Ensamblado
   en el navegador con los endpoints que ya existen; no se creó ningún
   endpoint de exportación nuevo. "Ver" ya estaba cubierto por el
   formulario de perfil de la misma página.
6. **Punto de entrada para corrección o supresión**: mismo
   `UserProfilePage.jsx`, un botón `mailto:` pre-llenado (nombre,
   documento, plantilla del cuerpo) al correo de la política. No es un
   formulario propio a propósito — no existe todavía a quién le
   llegaría ni quién lo atendería (ver punto 8 de abajo); fingir un
   formulario que no llega a ningún proceso real sería peor que no
   tenerlo.

### Backend

7. **Registro de consentimiento**: tabla + endpoint que guarde quién
   aceptó qué (general, salud, o ambos), cuándo, con qué versión del
   documento, y **por qué medio** (digital o físico — el registro
   manual usa un formato en papel firmado, no el checkbox; el sistema
   necesita poder registrar "consentimiento físico" como opción válida,
   no solo el digital). **Importante:** los tres checkboxes del
   frontend (§Frontend #2 y #3) hoy son solo un candado de UI — bloquean
   el botón de enviar, pero su estado **no se manda al backend**:
   `PersonRequest`/`autoregistro` no lleva ningún campo de consentimiento,
   así que si el proceso se cae después de marcarlos, no queda ningún
   rastro de que se marcaron. Falta decidir el contrato (¿un campo en el
   body de `POST /api/personas/autoregistro`? ¿un endpoint aparte tras
   crear la persona?) y conectarlo desde el frontend una vez exista.
8. **Canal y proceso de habeas data**: falta definir a quién le llegan
   las solicitudes y quién las atiende — hoy no hay ni endpoint ni
   proceso manual definido.
9. **Auditoría conserva valores personales antiguos como texto libre**
   (ejemplo: un teléfono corregido queda guardado el anterior para
   siempre). Decisión pendiente: ¿se deja así, o se sanea ante una
   solicitud de supresión?

---

## Decisiones de negocio pendientes (no son de código)

### 1. Proveedores tecnológicos reales

Aplazado a propósito hasta tener el sistema terminado — ahí se decide
con datos reales de costo y recursos (VPS tipo Hostinger vs. un PaaS
tipo Railway, según lo que consuma el backend real). La sección 7 de
la política se completa cuando esto se resuelva.

### 2. Archivo de los formatos físicos firmados

El registro manual usa un formato en papel firmado (ver
`autorizacion-fisica-registro-manual.md`) en vez del checkbox digital.
Falta decidir: ¿quién es responsable de archivarlos, dónde se guardan
físicamente, y por cuánto tiempo?

---

## Ya resuelto, no requiere más discusión

- **Plazos de conservación**: 10 años para todos los datos, sin
  distinguir financiero de no financiero. Decisión tomada.
- **Naturaleza legal de "Aeróbicos" dentro de PSR**: es un programa de
  Pioneros de la Salsa y Ritmos, no una entidad separada. Confirmado.
- **Versionado de la política**: se queda en v1 única por ahora, sin
  sistema de versiones. Decisión tomada.
- **Aviso al contacto de emergencia**: no se le notifica de forma
  proactiva (requeriría pedirle un dato nuevo — su correo — y sería
  desproporcionado para el propósito). La política pública + la
  declaración de quien lo registra + un canal reactivo (numeral 5 de
  la política) es la posición adoptada.
- **Fecha de publicación**: se fija el día real del despliegue, no
  antes.

Y dos verificaciones operativas antes de producción, ya identificadas
en la auditoría técnica: `SEED_ENABLED` debe quedar en `false`, y el
sitio debe servirse por HTTPS.
