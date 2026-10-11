# Próximas mejoras — no entran en v1, quedan anotadas para después

Nada de aquí bloquea la entrega. Se documenta ahora para no perderlo, no para hacerlo hoy.

---

## 1. `ResetPasswordPage` — mostrar de quién es la cuenta antes de pedir la contraseña nueva

Pedido explícito (`#problemas generales...md`): hoy el cambio de contraseña funciona bien, pero la
pantalla no muestra a quién pertenece la cuenta antes de pedir la nueva contraseña. Para v2:

- Al abrir el enlace con el token válido, mostrar arriba del formulario: número de documento,
  nombre y apellido de la persona dueña de esa cuenta (para que quien lo abre confirme que es su
  cuenta antes de escribir nada).
- Luego, el formulario normal: "Ingresa tu nueva contraseña" + "Confírmala".
- Esto necesita que el backend exponga esos tres datos junto con la validación del token (hoy
  `ResetPasswordPage` probablemente solo sabe que el token es válido, no de quién es) — a definir
  cuando se retome.

---

## 2. Checkin ligado de verdad a la sesión de clase

Lo más grande de `problemas-errores-checkin-sesion.md`, y el propio reporte ya dice "no sé si se
logre en v1". Requiere repensar el modelo de datos, no un parche:

- Tarifa ajustable por sesión (hoy es una sola tarifa global para todas las sesiones).
- Un checkin debe quedar amarrado a QUÉ sesión específica corresponde, para cuando hay dos
  sesiones en horarios cruzados en salones distintos.
- Al cancelar/eliminar una sesión, sus checkins no deberían seguir apareciendo como si la sesión
  siguiera activa.
- `reception/checkin` debería mostrar la fila de la sesión vigente en ese momento — vacío si no hay
  ninguna clase corriendo — y permitir consultar el historial de checkins de sesiones pasadas por
  separado, en vez de una sola lista plana de todos los tiempos.

> **Nota 20-sep:** el último punto (la fila de la sesión vigente, con las terminadas aparte) y la
> regla de qué pasa con un check-in cuando la sesión ya terminó pasaron a **v1**, porque probando se
> vio que siguen llegando y cobrándose personas sobre una sesión ya acabada. Está pedido al back en
> `pendientes-back-20-09.md` (punto 5). Lo que sigue en v2 es el resto: tarifa por sesión y
> amarrar cada check-in a una sesión específica con salones cruzados.

---

## 3. Abono peso a peso, no solo por días completos

Decisión para v1 (13-sep): cuando alguien paga de más, "Abonar a sesiones futuras" solo cubre días
COMPLETOS de tarifa — cualquier resto que no alcance para un día más se entrega siempre como
vuelto, para evitar líos de a cuánto equivale una fracción de sesión. Para v2: permitir que ese
resto quede como abono real a la cuenta (crédito parcial en pesos, no en días), en vez de forzar a
que todo excedente termine siendo vuelto. Requiere decidir cómo se "gasta" ese crédito parcial
después (¿se completa con más dinero después? ¿se muestra como saldo a favor en el perfil?).

---

## 4. UI de reembolso — explicar el tope en vez de solo recortarlo en silencio

Hoy, si alguien escribe un monto de reembolso mayor al abonado, `RefundModal.jsx` simplemente
recorta el número al máximo permitido sin decir nada. Pedido para v2: mostrar el motivo, tipo
"$4.500 fue lo que se recibió — no se puede reembolsar más de eso" (o, si ya se entregó vuelto
antes, explicar eso también), para que quien esté en el mostrador entienda por qué el número
cambió solo, en vez de pensar que es un bug.

---

## 5. Reembolso: endpoint real en vez de que el front reparta el monto entre movimientos

Decisión para v1 (13-sep, validada): `ReceptionCheckinPage.handleConfirmRefund` hoy junta todos los
movimientos de pago con saldo pendiente de un checkin y llama a
`POST /api/movimientos/{id}/reembolso` una vez por cada uno hasta cubrir el monto pedido — lógica
de negocio (orden, cuánto sale de cada movimiento) que vive en el front y no debería. Funciona,
pero es deuda técnica reconocida, no una decisión de diseño. Para v2: pedir al backend un endpoint
que reciba un monto total y decida él mismo contra qué movimiento(s) aplicarlo — ver
`NOTAS-PARA-BACK-FINAL.md` #1.

> **Nota 20-sep: resuelto.** El reembolso ya usa `POST /api/personas/{id}/reembolso` y el reparto lo hace
> el back. La deuda equivalente que sigue abierta es la del vuelto pendiente (ver el punto 10, A2).

---

## 6. Filtro de sesiones (ocultar canceladas, por fecha) — que lo resuelva el backend

Decisión para v1 (13-sep, validada): `SessionsPage.jsx` oculta las sesiones canceladas con un
filtro puramente de frontend (`sessions.filter(s => !s.cancelled)`), porque desde que las sesiones
se generan por adelantado la lista sin filtrar es inmanejable. Para v2: que
`GET /api/sesiones` acepte parámetros reales (`?cancelada=false`, rango de fechas), y el frontend
deje de filtrar una lista completa que ya trajo de más.

> **Nota 20-sep:** pasa a **v1**. La pantalla de Sesiones se rehace por semana, con filtro de estado
> real (activas por defecto) y rango de fechas para el histórico — pedido al back en
> `pendientes-back-20-09.md` (punto 6).

---

## 7. Términos y condiciones — adjunto por correo, y constancia de aceptación

Para la entrega, el enlace a una página de términos alcanza (cero cambios de backend — confirmado
por el back, incluso lo prefieren: el correo no engorda, una errata se corrige sin reenviar, se
esquivan filtros de spam). El back ya dejó la fontanería de adjuntos lista en `EmailSender` (interfaz
+ SMTP + Brevo) pero sin enganchar a ningún correo — falta el PDF, no el código, si se retoma en v2.

**Importante para la parte legal, aparte del adjunto**: si lo que se necesita es *constancia de que
la persona aceptó los términos* (quién y cuándo), ni el adjunto ni el enlace lo resuelven por sí
solos — eso es guardar la aceptación en la base, trabajo de backend. Confirmado explícitamente por
el back como pendiente de v2, no de hoy.

---

## 8. Reembolso cuando la persona pagó parcial y se retira

**Limitación de la versión actual (v1), anotada el 20-sep:** el reembolso solo existe para saldo
**a favor**. Si alguien paga, por ejemplo, 3.000 de una sesión de 5.000, queda debiendo 2.000 y
decide no dar la clase (se retira), el sistema responde "no hay saldo a favor que reembolsar" y no
hay forma de devolverle lo que entregó. Es un caso poco común (la persona ya pasó por registro y
check-in), pero existe.

**Para v2:** poder devolverle lo recibido a quien pagó y no tomó la clase — lo que implica deshacer o
anular su llegada de esa sesión y el cobro parcial, no solo permitir un reembolso contra un saldo
negativo. Requiere decidir cómo queda registrado (¿se anula la llegada?, ¿queda un reembolso ligado
a esa llegada?) y qué pasa con la asistencia si ya estaba confirmada.

---

## 9. Repartir el dinero recibido en partes

**Decisión del 20-sep:** en v1, cuando se recibe más que la tarifa, recepción elige **una** de tres
acciones directas: *Entregar vuelto*, *Abonar N días* o *Dejar vuelto pendiente*.

**Para v2:** poder **dividir** lo recibido en un solo paso — por ejemplo, de 20.000: 5.000 para la
sesión de hoy, 10.000 abonados como 2 días adelantados y 5.000 de vuelto pendiente — con un mini-panel
que reparta el monto antes de confirmar, en vez de obligar a escoger una sola de las tres.

---

## 10. Lógica y parches que hoy viven en el frontend (deuda para pasar al backend)

Criterio del producto (20-sep): **el servidor decide y el frontend solo muestra.** Esta lista reúne
todo lo que hoy el frontend calcula, decide o arma por su cuenta — casi siempre porque el backend
todavía no entregaba el dato resuelto. Ordenada por riesgo: primero lo que toca dinero. Cada punto dice
dónde está, por qué es un parche y qué debería entregar el servidor.

### A. Dinero — riesgo alto

**A1. Repartir lo recibido entre tarifa, adelanto y vuelto** — `ReceptionCheckinPage.handleCharge` y las
vistas previas de `QueueRow`. El frontend calcula cuántos días cubre el monto (`Math.floor(monto / tarifa)`),
cuánto se aplica (`amountApplied`) y cuánto sobra, y según el botón decide si entrega el vuelto llamando a
otro endpoint. Es el reparto del dinero, y depende de la tarifa que el frontend tiene en memoria.
*Debería:* una sola operación en el back ("cobrar" con el monto recibido y qué hacer con el excedente:
entregar, abonar o dejar pendiente) que reparta y responda el resultado.

**A1b. Cuánto debe pagar hoy la persona — ✅ resuelto (21-sep).** El servidor entrega `amountDue` y
`coveredByCredit` en cada fila y el frontend los usa **tal cual**; el cálculo provisional que había en
`reception/utils.js` se borró. Si el campo no llega, la fila no ofrece cobro (0) en vez de inventar un monto.
**Riesgo abierto:** el back validó esa cifra razonando sobre el modelo, sin ejecutarla; si un cobro sale raro,
empezar por comparar el `amountDue` de la fila con la tabla de `docs/test.md`.

**A2. Resolver el vuelto pendiente de una persona** — `ReceptionCheckinPage.findPendingMovements`,
`handleDeliverPending`, `handleApplyPending`. Las acciones del back son por movimiento, pero la fila solo
trae el total de la persona; el frontend busca los movimientos en `pendientes/vueltos`, los empareja con la
persona (por `personId` o, si no viene, **por nombre completo**, frágil con homónimos) y reparte el abono
entre ellos del más antiguo al más nuevo, llamando uno por uno. Es el mismo tipo de parche que ya se
corrigió con el reembolso. *Debería:* acciones por persona (`entregar-vuelto`, `abonar-vuelto`), pedido en
`pendientes-back-20-09-seguimiento.md` puntos 1 y 2. Además, "Abonar N días" calcula N en el frontend
(`floor(pendingChange / tarifa)`). Lo mismo con "Abonar N sesiones" del excedente de un cobro (`floor(excedente / tarifa)`).

**A3. Cuánto cuesta un pago por adelantado** — modal de "Pago por adelantado": el total es
`tarifa × días elegidos` calculado en el cliente, y los días ofrecidos son los próximos 10 (`getUpcomingDays`)
sin saber si hay una sesión esos días. *Debería:* el servidor calcula el precio de N días y dice cuáles
fechas son válidas.

**A4. Método de pago por defecto** — `pickDefaultPaymentMethodId` elige "efectivo" **buscando la palabra en el
nombre** del método. Si administración lo renombra, deja de funcionar en silencio. *Debería:* un método
marcado como predeterminado en el servidor (`isDefault`).

**A5. Textos que quedan como registro** — `handleCharge` arma el `note` del movimiento ("Cobro de entrada +
N sesión(es) por adelantado", "… (vuelto pendiente)"), que después aparece como subtítulo de la fila y en
auditoría. Es texto de negocio escrito por el cliente. *Debería:* generarlo el back a partir de la
operación.

**A6. "Recaudo de hoy" — ✅ resuelto (21-sep).** El encabezado del check-in muestra dos cifras con nombre,
`collectedThisSession` ("Esta sesión") y `collectedToday` ("Ingresó hoy"), que vienen ya calculadas en
`GET /api/cola/resumen`; el frontend ya no escoge entre reportes ni suma nada. Definiciones del back:
**esta sesión** = lo cobrado en las asistencias confirmadas de esa sesión (por la fecha de la clase);
**hoy** = todo lo abonado hoy, adelantos incluidos y menos reembolsos (por la fecha del pago). Un cobro de
50.000 con 45.000 de vuelto cuenta 5.000. No es el cajón físico: si hiciera falta, el back ofrece una tercera
cifra (`cashInDrawer`). Para un profesor vienen en `null` y no se muestra nada.

### B. Estados y permisos derivados en el cliente

**B1. Qué acciones están disponibles en una sesión — ✅ resuelto en Sesiones (21-sep).** `SessionsPage` y
`TeacherSessionsPage` pintan "Finalizar" y "Cancelar" con `canClose` / `canCancel` que calcula el servidor según
quién pregunta. **Sigue abierto en el check-in de recepción:** `GET /api/cola/resumen` trae esas banderas en
`null`, así que el botón "Finalizar sesión" del check-in se muestra a ADMIN, RECEPCIÓN y PROFESOR cuando la
sesión está en curso (un profesor puede verlo en una clase ajena y recibir 403, que se muestra como error).
*Debería:* `canClose` calculado en el resumen (y para el segundo lanzamiento, punto 13).

**B2. Cuál es "la sesión en curso" — ✅ resuelto (21-sep).** Sale de `GET /api/cola/resumen` (`session`, la
vigente según el reloj del servidor, aunque nadie haya llegado); ya no se elige de una lista.

**B3. "No hay clase en curso" y reinicio del check-in — ✅ resuelto (21-sep).** `GET /api/cola/admin` devuelve
solo la sesión vigente y `[]` si no hay ninguna viva, así que la pantalla se reinicia sola al terminar la clase;
se borró el apartado de filas terminadas ("Ver sesión terminada"). Queda solo una guarda de presentación en
`QueueRow`: una fila con `sessionStatus` `ENDED` no ofrece cobro.

**B4. Si la llegada del miembro sigue vigente y si "ya pagó hoy" — ✅ en gran parte resuelto (21-sep).** El
mensaje sale de `paymentStatus` (`PAGADA_POR_ADELANTADO`, `PAGADA`, `PENDIENTE`) y `amountDue` del check-in propio.
Queda un respaldo: si `paymentStatus` no llega, "pago al día" se deduce de `amountCharged > 0`. Y el frontend
sigue ocultando la llegada de una sesión `ENDED`/`CANCELLED` para ofrecer marcar otra (usa el estado del servidor).

**B5. Quién es personal** — `STAFF_ROLES` (`constants/roles.js`) repite en el cliente qué roles pueden
leer la cola, personas y sesiones, para no pedir lo que dará 403. Si cambian los permisos, el cliente queda
desactualizado. *Debería:* el login devuelve los permisos/capacidades de la cuenta.

**B6. Clasificación de cuenta con respaldo** — `getAccountStatus` usa el `accountStatus` del servidor, pero
si el campo no llega cae a `balance < 0 → DEBE`. Es un respaldo para no esconder una deuda; se quita cuando
el campo sea siempre confiable.

### C. Datos armados en el cliente

**C1. Totales de cada sesión — ✅ resuelto (21-sep).** `GET /api/sesiones` y `/mias` traen `totalCheckins` y
`totalAttended`; se borró el cruce con `/resumenes`.

**C2. La semana lunes–domingo** — `weekUtils.getWeekRange` calcula en el navegador qué fechas son la
semana (con su hora local). *Debería:* el back acepta "semana N" o el frontend solo manda un rango; hoy es
solo presentación, pero la frontera de la semana depende del reloj del usuario.

**C2b. Ventana de sesiones ±7 días** — `DataContext` pide siempre 7 días atrás y 7 adelante para poder
etiquetar filas y detectar la sesión en curso; es un número mágico del cliente.

**C3. Descarga de datos personales en PDF** — `personalDataPdf.js` arma el documento en el navegador con
cuatro endpoints, y la sección de **Consentimientos** se renderiza de forma defensiva porque no se conoce
la forma exacta de esa respuesta. *Debería:* un endpoint de exportación en el back (con el formato
oficial y la constancia de qué se entregó y cuándo).

**C4. Nombre del profesor en la lista de sesiones — ✅ resuelto (21-sep).** La sesión trae `teacherName`; se
borró el cruce con la lista de profesores (la lista solo se pide para el selector de "Asignar/Cambiar") y con
él el retraso "…" al recargar.

### D. Reglas duplicadas del backend

**D1. Edad mínima 18** — `BirthDateField` (por defecto y en cada formulario) repite el número que también
valida el back. Si el back cambia la regla, el selector queda desalineado. *Debería:* el back la expone
como configuración pública.

**D2. Validaciones de formulario** — teléfono de mínimo 10 dígitos y documento de mínimo 6
(`utils/validation.js`), el patrón del teléfono del contacto de emergencia, el máximo de 5 contactos y de
2000 caracteres (`RegistrationHealthAndContacts`). Son solo para dar aviso inmediato — el back valida igual —
pero son cinco copias que hay que mantener sincronizadas.

### E. Presentación provisional

**E1. Observaciones de salud: chips y texto libre** — `MemberEmergencyContactPage.parseSavedHealth`
reconstruye a partir de un texto plano si lo guardado fue elegido de una lista o escrito a mano
(adivinando por el separador `", "`). Es frágil. *Debería:* o se guarda estructurado (condiciones + otro), o
se deja solo texto libre.

**E3. Programación con horizonte de una semana y botón "Regenerar programación"** (21-sep) — `SessionsPage`.
El back genera las clases de los horarios recurrentes **hoy + 6 días** (decisión de producto): cuánto de "la
semana que viene" existe depende del día en que se mire. La pantalla avisa que lo posterior no está programado
(`SCHEDULE_HORIZON_DAYS = 6`, copiado del servidor: parche; debería llegar como dato, `horizonDays`) y marca esos
días "Aún sin programar". El botón "Regenerar programación" (solo ADMIN, `POST /api/horarios/generar-sesiones`,
idempotente) es de rescate; también está en Horarios.

**E2. Filtros del check-in** — "Sin atender" por defecto y contadores por filtro se calculan en el cliente
sobre la cola ya entregada. Es presentación aceptable, pero cambia si la cola se pagina.

### F. Parches de la ronda del 21-sep (a 6 h del deploy)

**F1. Relectura de la cola como red de seguridad** — `ReceptionCheckinPage`. Las señales del WebSocket ya llegan
(el back corrigió que la llegada propia no avisaba y agregó el aviso cuando una sesión empieza o termina por el
reloj, con hasta ~1 minuto de retraso). Aun así el frontend relee la cola (y con ella el resumen) **cada 30 s y
al volver a la pestaña**, por si el WebSocket no conecta en el despliegue (por ejemplo un back en `http` detrás de
una página `https`, que el navegador bloquea). Es un parche; se quita cuando se confirme que la conexión en vivo
es confiable en producción. Para saber si es la señal o el relleno el que actualiza: una llegada que aparece en
menos de ~5 s es la señal; una que tarda hasta 30 s es el relleno.

**F2. Saldo de quien pagó una parte antes de confirmar** — la fila muestra "Falta $X" (el `amountDue` del servidor)
en la columna de saldo cuando la persona ya abonó algo pero aún debe la sesión, en vez de "A favor $1.000" (el
descuento de la tarifa se hace al confirmar la asistencia). Es solo presentación con el dato del servidor; el
saldo real no cambia.

**F3. Textos "sesiones" en vez de "días"** — solo etiquetas (miembro y recepción); los números no cambian.

> **Regla de trabajo:** cada vez que el frontend implemente algo que sea un parche o lógica que debería vivir
> en el back, se anota aquí en el mismo momento, con dónde está y qué debe entregar el servidor.

---

## 11. Reportes: el filtro de "saldos por persona" se aplica sobre una sola página

**Anotado el 21-sep.** En Reportes → saldos de personas, el filtro (por ejemplo "con saldo a favor") se
aplica en el navegador **solo a la página que ya se cargó** (`ReportsPage`, `filteredAccounts`). Si hay 5
personas con crédito y todas caen en la página 2, al filtrar la página 1 sale vacía y parece que no hay
nadie. *Para v2:* que el filtro sea un parámetro del servidor (`?tipo=favor|deuda|todos`) y la paginación
se calcule sobre lo ya filtrado; mientras tanto, mostrar "hay resultados en otras páginas" o traer todo.

## 12. Cobrar con monto 0 (dejar toda la sesión como deuda)

**Anotado el 21-sep.** Hoy el campo de monto no acepta 0: el botón de cobrar queda inactivo. Para el caso
de quien debe **toda** la sesión (llega, la toma y paga después), en v2 debe poder registrarse "recibido
$0": confirmar la asistencia y dejar la deuda completa, sin tener que inventar un pago. Hoy la salida es
"Confirmar asistencia" sin cobro.

## 13. "Finalizar sesión" solo para quien da la clase

**Anotado el 21-sep.** Al lanzar, el botón lo ven ADMIN, RECEPCIÓN y PROFESOR mientras haya una clase en curso.
Para el segundo lanzamiento: que lo vea solo el **profesor asignado a esa sesión** (y el admin), lo que necesita que el back
mande `canClose` por sesión según quien pregunta (ya anotado en 10.B1).

## 14. La TV de la fila ignora "personas por turno" de Configuración

**Anotado el 26-sep (a horas del deploy).** El dueño pidió llamar de a **1** persona. En Configuración de la
cola el valor ya estaba en 1, pero la TV (`/lobby/queue`) seguía llamando de a 2.

**Causa:**

- `LobbyQueueDisplayPage.jsx` recibe `peoplePerTurn` como prop con valor por defecto fijo, y la ruta de
  `App.jsx` lo monta sin pasarle nada (`<LobbyQueueDisplayPage />`). La TV **nunca** consulta
  `GET /api/cola/configuracion`; ese valor solo lo lee y guarda `QueueConfigTab.jsx`.
- Aunque la TV lo pidiera, `QueueConfigController` exige `Authorities.STAFF` (ADMIN, TEACHER,
  RECEPTIONIST). Si la TV corre con la cuenta de la Terminal PSR (`PSR_TERMINAL`) recibiría 403. El
  comentario del propio controlador dice "La sala necesita leerla", así que el permiso quedó corto;
  `/api/cola/espera` y `/api/cola/prepagada` sí usan `TERMINAL_QUEUE`.

**Parche de v1:** el valor por defecto pasó de 2 a **1** en `LobbyQueueDisplayPage.jsx`. Si se cambia en
Configuración, la TV **no** se entera: hay que cambiar el código.

**Corrección propuesta para v2:**

- *Frontend* (`LobbyQueueDisplayPage.jsx`): quitar el número fijo; leer `peoplePerTurn` de
  `GET /api/cola/configuracion` al cargar y volver a leerlo en cada señal del WebSocket (la misma que ya
  relee las listas), para que un cambio del admin se vea sin recargar la TV. Si la lectura falla, usar 1 y
  dejar el error en consola.
- *Backend* (`QueueConfigController`): el `GET` pasa a `Authorities.TERMINAL_QUEUE` (incluye
  `PSR_TERMINAL`); el `PUT` sigue solo para ADMIN. La respuesta no tiene datos sensibles (personas por
  turno y dos valores en minutos).
- *Opcional:* que guardar la configuración emita una señal por WebSocket (`/topic/cola/espera`) para que la
  TV la relea al instante, en vez de esperar al próximo movimiento de la fila.
- *Pendiente de confirmar:* con qué cuenta se abre la TV del local (Terminal PSR o admin/recepción).

**Probar:** abrir la TV con la cuenta real del local, confirmar que llama de a 1, cambiar a 2 en
Configuración y ver que la TV se ajusta sola.

## 15. Sesión de 30 días sin renovación: volver a un token corto con refresh

**Anotado el 28-sep.** El token de sesión duraba 1 hora y no hay renovación automática, así que la TV de la
fila y recepción volvían al login cada hora. Para v1 se subió `jwt.expiration-in-ms` a **30 días**
(`2592000000`) en `application.properties` del backend. Es provisional.

**Riesgos de dejarlo así:**

- Cerrar sesión solo borra el token del navegador; el token sigue siendo válido 30 días. Si alguien lo copia,
  puede usarlo hasta que venza.
- Mitiga en parte que el backend relee el usuario de la base en cada petición: una cuenta desactivada o sin el
  rol deja de tener acceso aunque su token no haya vencido.

**Para v2:**

- Backend: `POST /api/auth/refresh` y `POST /api/auth/logout`, un *refresh token* guardado (y revocable) y
  `expiresIn` en la respuesta de login. Volver el token de acceso a algo corto (15–60 min).
- Frontend: encender `VITE_AUTH_REFRESH_ENABLED=true` (el código ya está en `apiClient.js` / `AuthContext.jsx`).
- **Ojo:** no activar el refresh con el token de 30 días. `scheduleProactiveRefresh` usa `setTimeout`, que en el
  navegador no acepta esperas de más de ~24,8 días: con 30 días dispararía la renovación de inmediato, en bucle.
  El token de acceso tiene que volver a ser corto antes de encender la bandera.

## 16. Quitar el botón "Abonar N sesiones"

**Anotado el 29-sep.** *Descripción pendiente.*

Hoy hay dos botones con ese texto en `QueueRow.jsx`: el del excedente de un cobro ("Abonar {extraDays}
sesión(es)") y el del vuelto pendiente ("Abonar {daysFromPending} sesión(es)"). Falta definir cuál (o
ambos) se quita y qué opción queda en su lugar.

## 17. Lobby: texto de "No pudimos ingresarte a la fila"

**Anotado el 29-sep.** *Descripción pendiente.*

El mensaje actual es "No pudimos ingresarte a la fila automáticamente. Pide en recepción que te agreguen."
y aparece en `LobbyRegistrationPage.jsx` y también en `MemberRegistrationPage.jsx`.

## 18. Historiales

**Anotado el 29-sep.** *Descripción pendiente.*
