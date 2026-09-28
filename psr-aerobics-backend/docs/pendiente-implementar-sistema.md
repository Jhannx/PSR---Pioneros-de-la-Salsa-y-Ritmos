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

7. ~~**Registro de consentimiento**~~ — **hecho (17-sep-2026)**. Tabla
   `consentimiento` (en `V1__INITIAL_DDL.sql`) y módulo `modules/consent`.
   Guarda quién aceptó, qué de los cuatro tipos, cuándo, con qué versión
   de la política y por qué medio — `DIGITAL` o `FISICO`, que es lo que
   permite dejar constancia del alta manual con formato en papel.

   **El contrato elegido: un campo en el body de cada endpoint**, no un
   endpoint aparte. Un endpoint aparte llamado antes o después de la
   acción reintroduce el mismo agujero — dos llamadas se pueden partir
   por la mitad. Así, el consentimiento se graba en la misma transacción
   que la operación: o quedan las dos cosas, o no queda ninguna.

   El backend **valida**, no solo registra: autoregistro sin las dos
   casillas, observaciones de salud con contenido sin la del numeral 4.2,
   y contacto de emergencia sin la del numeral 5 responden 400. El
   candado del frontend se sigue saltando llamando a la API directamente;
   este no.

   Detalle de cada campo, en `docs/NOTAS-PARA-FRONTEND.md` (escrito el
   18-sep: estaba referenciado desde aquí y desde `PENDIENTES.md` pero no
   existía, que es por lo que el reporte del 18-sep decía "no veo ese
   código"). Requiere un cambio en `PersonFormModal.jsx` que la decisión de
   alcance original había descartado — está explicado ahí.

   **Ampliación del 18-sep:** los tres consentimientos también se pueden
   enviar dentro del alta (`healthObservations` + `healthDataConsent` y
   `emergencyContacts[]` en `PersonRequest`), porque tres de las cuatro
   pantallas de registro son públicas y quien las usa no tiene token con
   el que llamar a los endpoints sueltos. Mismos servicios, mismas reglas,
   misma transacción. Y el medio dejó de asumirse `DIGITAL`: lo decide
   quien llama, así que un alta de mostrador queda registrada como
   `PHYSICAL` en vez de declarar una casilla que nadie marcó.

   Tres decisiones que quedaron tomadas de paso:
   - **No se guarda IP ni user-agent.** El numeral 2.8 de la política ya
     publicada declara qué se recoge, y la IP no está en esa lista.
   - **A las personas ya registradas no se les inventó una fila.** El
     `GET` devuelve `registrationConsentMissing: true` para que se les
     pueda preguntar. Fabricar el consentimiento habría sido peor que no
     tenerlo.
   - **Revocar el de salud borra `observaciones_salud`** en la misma
     transacción. Revocar el general **no borra nada**: eso es una
     solicitud de supresión, y choca con los 10 años del numeral 8.1.
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
en la auditoría técnica: `SEED_TEST_ENABLED` debe quedar en `false`, y el
sitio debe servirse por HTTPS.
