# Notas para Frontend

Lo que cambió en el contrato de la API y hay que conectar. Siete bloques: el
**consentimiento** (17-sep) y las rondas del **18**, **19**, **20**, **20/21**,
**21** y **21 de septiembre (noche)**, cada una respondiendo uno por uno a su
documento de pendientes.

Este documento estaba referenciado desde `PENDIENTES.md` y desde el punto 7 de
`pendiente-implementar-sistema.md` pero nunca se escribió — de ahí que el reporte
del 18-sep dijera "no veo ese código". Ahora sí está.

---

# Bloque 1 — Consentimiento (17-sep)

El contrato elegido es **un campo en el body de cada endpoint**, no un endpoint
aparte llamado antes o después. Dos llamadas separadas se pueden partir por la
mitad y volvería a quedar una persona sin constancia, o al revés; así el
consentimiento se graba en la misma transacción que la operación.

**El backend valida, no solo registra.** Saltarse la casilla llamando a la API
directamente ya no funciona: responde 400.

## 1.1 Alta de persona — `POST /api/personas/autoregistro` y `POST /api/personas`

Tres campos nuevos, todos `Boolean`. Cuál hace falta depende de la ruta:

| Campo | Autoregistro (público) | Alta de mostrador |
|---|---|---|
| `dataProcessingConsent` | **obligatorio `true`** | ignorado |
| `voluntaryParticipationConsent` | **obligatorio `true`** | ignorado |
| `signedPhysicalConsent` | ignorado | **obligatorio `true`** |

Los mensajes de error son distintos para cada casilla, así que se pueden pintar
sobre el campo que falta.

> **Esto obliga al cambio en `PersonFormModal.jsx`** que la decisión de alcance
> original había descartado. No es una casilla de aceptación: es la confirmación
> de quien está en el mostrador de que **recogió el formato físico firmado**. Sin
> ella el alta responde 400. El medio queda registrado como `PHYSICAL`, que es lo
> que permite distinguir después un alta en papel de una digital.

## 1.2 Observaciones de salud — `PUT /api/personas/{id}/observaciones-salud`

```jsonc
{ "healthObservations": "Asma leve", "healthDataConsent": true }
```

- Con contenido y sin `healthDataConsent: true` → **400**.
- Vaciar el campo (`null` o `""`) **no exige nada** y además revoca el
  consentimiento anterior. Pedir permiso para dejar de tratar un dato sería al
  revés, y el numeral 4.2 dice que no suministrarlo no puede costarle nada a la
  persona.
- El consentimiento solo se re-registra en la **transición**, no en cada guardado:
  corregir una coma no genera una fila nueva.

## 1.3 Contacto de emergencia — `POST /api/personas/{id}/contactos-emergencia`

```jsonc
{ "name": "María Pérez", "phone": "3001234567", "thirdPartyConsent": true }
```

- Sin `thirdPartyConsent: true` → **400**.
- **Una fila de consentimiento por contacto**, no una por persona: cada contacto
  es un tercero distinto, y estar autorizado a entregar los datos de uno no dice
  nada sobre el otro.
- El `PUT` que edita un contacto **no** lo exige: corregir un dígito no suministra
  datos de nadie nuevo.

## 1.4 Consulta y revocación

```
GET  /api/personas/{id}/consentimientos     STAFF + el propio titular
POST /api/personas/{id}/consentimientos     DESK  + el propio titular
```

El `GET` devuelve el estado vigente de los cuatro, el historial completo y:

```jsonc
{
  "personId": 1,
  "policyVersion": "v1",
  "dataProcessing": true,
  "voluntaryParticipation": true,
  "healthData": false,
  "thirdPartyData": true,
  "registrationConsentMissing": false,
  "history": [ /* del más reciente al más antiguo, revocaciones incluidas */ ]
}
```

**`registrationConsentMissing: true`** significa "a esta persona nunca se le
preguntó" — es todo el mundo que ya estaba registrado antes de que la tabla
existiera. No es lo mismo que haberlo revocado, y por eso se distingue: a esos hay
que preguntarles. No se les fabricó una fila a propósito.

El `POST` es para eso y para revocar. **No lo uses para el flujo normal** — el
consentimiento del día a día viaja dentro de la operación que lo motiva:

```jsonc
{ "type": "HEALTH_DATA", "granted": false, "medium": "DIGITAL" }
```

- `type`: `DATA_PROCESSING` · `VOLUNTARY_PARTICIPATION` · `HEALTH_DATA` ·
  `THIRD_PARTY_DATA`
- `medium`: `DIGITAL` · `PHYSICAL` (en el JSON va el nombre de la constante, no el
  valor en español que se guarda en la base)
- Siempre responde **201**: revocar no borra la fila anterior, añade una nueva.

Dos cosas que conviene saber:

- **Revocar `HEALTH_DATA` borra `observaciones_salud`** en la misma transacción.
- **Revocar `DATA_PROCESSING` no borra nada.** Eso es una solicitud de supresión y
  choca con los 10 años del numeral 8.1; el canal para eso está sin definir (punto
  8 de `pendiente-implementar-sistema.md`).

---

# Bloque 2 — Ronda del 18-sep

Respuesta punto por punto a `docs/pendientes-back.md`. **Todo lo de abajo ya está
implementado**, salvo donde se diga otra cosa.

## A1 — Teléfono duplicado al actualizar ✅ corregido

Las dos partes:

1. `PersonService.update` ahora compara antes de validar, igual que ya hacía el
   documento. Reenviar el teléfono actual sin tocarlo ya no dispara nada.
2. El seed le da un teléfono distinto a cada persona, derivado de su documento
   (`3000001111`, `3000004444`…). Con el literal repetido, cualquier alta o
   edición de prueba chocaba con las otras veintiuna cuentas.

**No hace falta cambiar nada en el frontend.**

## A2 — Un miembro no puede editar su perfil ✅ resuelto — **Opción 1**

Endpoint nuevo:

```
PUT /api/personas/{id}/mis-datos       DESK + el propio titular
```

```jsonc
{
  "firstName": "Ana",
  "lastName": "Pérez",
  "phone": "3001234567",
  "birthDate": "1990-05-14"
}
```

Los cuatro son obligatorios (es un reemplazo, como el `PUT` grande). Devuelve el
`PersonResponse` completo, igual que `PUT /api/personas/{id}`.

**Documento y tipo de documento no están en el DTO**, así que no hay forma de
tocarlos por esta vía — ni por descuido ni a propósito. El `PUT /api/personas/{id}`
sigue siendo de mostrador y sigue siendo el que reescribe el documento.

Sigue abierto a `DESK` además del titular: recepción corrige fichas ajenas todos
los días, y obligarla a usar el `PUT` completo para arreglar un teléfono solo la
empujaría hacia el endpoint más peligroso.

> **Para "Mi Perfil": cambia el `PUT /api/personas/{id}` por este y ya no hace
> falta ocultarle el botón de guardar a quien no sea ADMIN/RECEPCIÓN.**

## A3 — El horario recurrente no generaba su sesión ✅ corregido

Tres cosas:

1. **`POST /api/horarios`, `PUT /api/horarios/{id}` y `PATCH /{id}/activar`
   generan las sesiones de ese horario al terminar.** Se hace justo después de
   confirmar la transacción, así que cuando llega la respuesta las sesiones ya
   están. Se acabó la ventana de espera.
2. El barrido automático pasó de **cada 6 horas a cada hora**.
3. Existe el endpoint manual que pedías:

```
POST /api/horarios/generar-sesiones     ADMIN
→ 200 { "message": "3 sesión(es) creada(s)." }
```

Es idempotente: lo que ya existe no se duplica. **Ponle el botón "Regenerar
sesiones"** en la pantalla de Horarios.

Al **modificar** un horario se generan las sesiones del horario nuevo, pero las
del anterior **no se borran**: pueden tener gente en la cola, y cancelarlas es una
decisión de quien administra, no un efecto secundario.

## A4 — Nota vieja en la fila tras un reembolso ✅ corregido — opción (a)

`sp_registrar_reembolso` ahora **hereda el `llegada_id` del abono original**, así
que el reembolso entra en el `LATERAL` de `vista_fila_hoy` y la nota de la fila
refleja lo último que pasó.

**Puedes seguir usando `note` como subtítulo de la fila.** (Si el abono original
tampoco colgaba de ninguna llegada —un pago por adelantado— el reembolso hereda
`NULL`: se hereda lo que haya, no se inventa.)

Efecto secundario que conviene conocer: como el reembolso ahora cuelga de una
llegada, **resta del recaudo de esa sesión** en `vista_recaudo_por_sesion` y
`vista_recaudo_diario`. Es más correcto que antes —devolver dinero baja la caja—,
pero si alguna pantalla daba por hecho que el recaudo solo sube, ahí está el
motivo.

## A5 — `saldo` ≠ `available` ✅ resuelto — campo nuevo

Se llama **`refundable`** y viaja en dos sitios:

```
GET /api/personas/{id}/saldo   → { ..., "balance": 54000, "refundable": 54000 }
GET /api/cola/admin            → cada fila lleva "refundable"
```

Sale de la **misma fórmula** que usa la validación del reembolso, así que ya no
hay dos números.

> **Usa `refundable` como tope del modal, no `balance`.** No son lo mismo:
> `balance` descuenta lo consumido en asistencias y `refundable` no. Quien abonó
> 50.000 y ya asistió 3 días tiene poco `balance` y bastante `refundable`.

Va también en la fila de la cola justo para que el modal, que se abre desde ahí,
no necesite una segunda petición.

## A6 — `change-password` ✅ nada que cambiar

El endpoint está bien y deja la línea `Contraseña cambiada por el propio usuario
ID {}` en el log. Con el arreglo del banner fuera de pantalla, el punto se cierra.
Si en la próxima prueba **no** aparece esa línea, avisa y lo miramos.

## B1 — Brevo ✅ el log ya dice la causa

Las dos correcciones están hechas:

- `BrevoApiEmailSender` mete **el código HTTP y el cuerpo de la respuesta de
  Brevo** en el mensaje de la excepción.
- `AuthEmailListener` pasa la excepción entera al `log.error`, así que hay traza.

Con eso, el próximo fallo dice si es la key (401), la cuota diaria (402), el
remitente sin verificar (400) o la IP (403). **La causa raíz sigue pendiente de
mirar en el panel de Brevo** — esto solo la hace visible.

## C1 — Contactos y salud en el registro ✅ **entra en v1**, ya está

`PersonRequest` acepta tres campos opcionales más, en **las dos rutas de alta**
(`POST /api/personas` y `POST /api/personas/autoregistro`):

```jsonc
{
  // ... lo de siempre, más los consentimientos del bloque 1
  "healthObservations": "Asma leve",
  "healthDataConsent": true,
  "emergencyContacts": [
    { "name": "María Pérez", "phone": "3001234567", "thirdPartyConsent": true }
  ]
}
```

Las reglas son **exactamente las de los endpoints sueltos**, porque se llama a los
mismos servicios:

- `healthObservations` máx. 2000 caracteres; con contenido exige
  `healthDataConsent: true` → si no, **400**.
- `emergencyContacts` máx. 5; cada uno con **teléfono obligatorio** (con formato) y
  **`thirdPartyConsent: true`** → si no, **400**.
- Todo en la **misma transacción del alta**: si un contacto viene mal, no queda una
  persona registrada a medias — no queda nada.

Ausentes o `null` no exigen nada: siguen siendo opcionales.

Un detalle sobre el medio: en el alta de mostrador estos consentimientos se
registran como `PHYSICAL`, no `DIGITAL`, porque ahí lo que autoriza es el formato
firmado y no una casilla que nadie marcó en pantalla.

## C2 — PDF ✅ nada que hacer

Confirmado: no hace falta endpoint de exportación.

---

# Bloque 3 — Ronda del 19-sep

Respuesta punto por punto a `docs/pendientes-back-19-09.md`. **Los cuatro están
implementados.**

## 1 — `reembolsable` mal calculado ✅ corregido

**Tenían razón, y la decisión que expliqué en A5 estaba mal.** Dije que
`refundable` no descontaba lo consumido en asistencias "a propósito"; con datos
reales eso deja la cuenta debiendo, que es justo lo que ustedes reprodujeron. La
regla correcta es la que pidieron:

> **reembolsable = el menor entre "lo abonado que sigue sin devolverse" y "el saldo
> a favor"**, y 0 si el saldo es cero o negativo.

Para la fila del reporte (abonado 15.000, cobrado 10.000, saldo 5.000)
`reembolsable` ahora devuelve **5.000**.

Lo tres puntos que pidieron:

1. El campo ya sale con el tope correcto.
2. **La validación usa exactamente ese número.** `POST /api/personas/{id}/reembolso`
   responde **400** si se pide más, con el mensaje "la persona solo tiene N
   disponible para devolver".
3. El valor es el mismo en `GET /api/personas/{id}/saldo` y en cada fila de
   `GET /api/cola/admin`.

Lo que garantiza el punto 3 no es disciplina: la fórmula vive ahora en **una sola
función de base de datos** (`fn_saldo_reembolsable`) que usan la vista y la
validación. Antes estaba escrita dos veces — por eso pudieron divergir.

> **Pueden quitar el recorte del frontend.** Ya no hace falta tomar el menor entre
> `saldo` y `reembolsable`: `refundable` ya viene acotado. Dejarlo tampoco rompe
> nada, pero deja de tener sentido.

## 2 — Métodos de pago

### 2.1 Endpoint de solo activos — **ya existía, no hacía falta uno nuevo**

```
GET /api/metodos-pago?activo=true     → solo los activos
GET /api/metodos-pago                 → todos, con su campo `active` (administración)
```

Es el mismo endpoint con un parámetro, y devuelve la misma forma de datos. **Esa es
la ruta exacta**: cambien la URL del selector y pueden quitar el filtro del
cliente. No creé `/activos` a propósito — serían dos rutas para lo mismo.

### 2.2 Rechazo de métodos inactivos ✅ añadido

No, no lo hacían. Ahora sí: responden **400** con un mensaje que nombra el método
("El método de pago 'X' está desactivado y no se puede usar para registrar
dinero"). Cubre los cuatro sitios que reciben un `paymentMethodId`:

- `POST /api/movimientos`
- `POST /api/movimientos/{id}/reembolso`
- `POST /api/personas/{id}/reembolso`
- `PATCH /api/movimientos/{id}` (la corrección de un movimiento)

Un id que no existe responde **404**, como antes.

### 2.3 Método de pago en el vuelto — **hereda el del original, por construcción**

Confirmado, y no hacía falta tocar nada: `POST /api/movimientos/{id}/vuelto` **no
crea un movimiento nuevo**. Solo marca `vuelto_entregado = true` sobre el abono que
ya existe, así que el método es el de ese abono necesariamente — no hay una
segunda fila que pudiera tener otro.

Si alguna vez hace falta entregar el vuelto por un medio distinto del que se
cobró (cobro por transferencia, vuelto en efectivo), eso sí exigiría una columna
nueva. Queda anotado en `PENDIENTES-V2.md`; díganme si el caso ocurre de verdad.

## 3 — Cancelar una sesión no avisaba ✅ corregido

Se publica el evento de cola por el canal que ya escuchan, con un tipo nuevo:

```jsonc
{ "sessionId": 42, "type": "SESSION_CHANGED" }
```

Va a **los tres topics** (`/topic/cola/admin`, `/topic/cola/espera`,
`/topic/cola/prepagada`), porque cancelar vacía las tres vistas de golpe. Como los
demás eventos, es solo una señal: hay que releer el endpoint REST.

Se publica al **cancelar** (`DELETE /api/sesiones/{id}`), y también al **cerrar** y
al **crear** una sesión, que era lo que pedían como "idealmente".

> Si el `switch` del cliente sobre `type` no tiene rama por defecto, **añadan
> `SESSION_CHANGED`** o el mensaje se ignora en silencio.

## 4 — Seed de operación lunes a viernes ✅ hecho

Seeder aparte, **apagado por defecto**:

```
SEED_HISTORICAL_ENABLED=true     # (app.seed.historical.enabled)
```

Depende del seed de cuentas (`SEED_ENABLED=true`): necesita un ADMIN y un PROFESOR
existentes para atribuirles las asistencias y los movimientos. Si no los encuentra,
avisa en el log y no escribe nada.

Qué carga, con **fechas relativas a hoy** y saltando sábados y domingos:

| | |
|---|---|
| **45 personas** | documentos `70001`–`70045`, contraseña = documento, teléfonos distintos, una de cada cinco sin correo |
| **5 días hábiles pasados** | sesión cerrada con profesor, **30 asistencias** + 4 llegadas sin confirmar cada uno |
| **Sesión de hoy** | abierta, 6 confirmadas y 6 esperando en la fila |
| **Sesión cancelada** | hoy, con 3 check-ins colgando — el caso del punto 3 |
| **Excepción de horario** | en el próximo día que coincida con un horario activo |
| **3 métodos de pago** | EFECTIVO y TRANSFERENCIA activos, **BONO REGALO desactivado** para probar el punto 2 |
| **Dinero** | cobros del día, abonos por adelantado, vueltos entregados y pendientes, reembolsos parciales |

Los casos límite que pidieron, cada uno en su propia persona (las últimas del
rango, para que no se mezclen con la rotación de asistentes):

| Documento | Estado |
|---|---|
| `70041` | **El del reporte**: abonó 15.000, consumió 10.000, saldo 5.000 — `refundable` debe decir 5.000 |
| `70042` | Saldo a favor alto (80.000) sin consumir |
| `70043` | **Debe**: asistió sin pagar, saldo negativo, `refundable` en 0 |
| `70044` | Con un reembolso parcial ya hecho (abonó 40.000, se le devolvieron 16.000) |
| `70045` | Saldo exactamente en **cero** |

Es idempotente por la vía simple: si la persona `70001` ya existe, no hace nada.
Para recargar hay que limpiar a mano — el orden de borrado está en el javadoc de
`HistoricalDataSeeder.alreadySeeded()`.

La coherencia sale sola y no de cuadrar números a mano: cada asistente tiene un
perfil de pago (paga el día, prepaga, paga con vuelto, o no paga) y el saldo es
siempre `abonado − cobrado` sobre las filas reales.

---

# Bloque 4 — Ronda del 20-sep

Respuesta punto por punto a `docs/pendientes-back-20-09.md`. **Los 9 están
atendidos**; el 8 y el 9 no necesitaban código (ver ahí por qué).

Se asume el criterio que fijaron: **el servidor decide y entrega el dato resuelto**.
Por eso hay tres campos nuevos que son estados, no números para comparar.

## 1 — El check-in propio no se recuerda al recargar ✅

Endpoint nuevo:

```
GET /api/checkin/propio        MIEMBRO (la persona sale del token)
→ 200 {
    "checkinId": 91, "sessionId": 42,
    "arrivalTime": "2026-09-20T18:05:00-05:00",
    "queuePosition": 3,
    "attended": false,
    "amountCharged": null,
    "sessionStatus": "IN_PROGRESS"
  }
→ 204  si hoy no ha marcado llegada
```

**204 y no 404**: "hoy no has llegado" es una respuesta normal a una pregunta
válida, no un error. No pinten un mensaje de fallo con eso.

Y el `POST /api/checkin/propio` **ya es seguro de repetir**, con el mismo cuerpo de
respuesta que el `GET`:

- **201** — se registró la llegada ahora.
- **200** — ya la tenía; se devuelve la misma, no se crea una segunda.

Ojo a un matiz que sí importa: se reutiliza la llegada de hoy **solo si su sesión no
ha terminado**. El día que haya dos clases, quien vino por la mañana puede marcar su
llegada a la de la tarde con normalidad.

> El `POST` cambia de tipo de respuesta: antes devolvía `CheckinResponse`, ahora
> `OwnCheckinResponse` (el de arriba). Trae más cosas — turno, atendido y estado de
> la sesión.

## 2 — "Terminal PSR" cuando no lo fue ✅

Confirmado: reutilizaba `DOCUMENTO`. **Ahora guarda `PROPIO`**, un valor nuevo del
enum de check-in.

| `checkinMethod` | Qué mostrar |
|---|---|
| `SELF` | **App / miembro** — lo marcó la persona desde su pantalla |
| `DOCUMENT` | Terminal PSR (kiosko por documento) |
| `QR` | QR |
| `MANUAL` | Manual (recepción) |

Sobre el nombre: se llama "propio" y no "QR" ni "app" a propósito. El QR de la sala
es una URL que abre esa misma pantalla, así que **el servidor no puede distinguir**
"entró por el QR" de "abrió la app": las dos llegan por el mismo endpoint con el
mismo token. Lo único que consta es que lo marcó la persona.

**Y el "Agregar manual" de recepción por documento:**

```
POST /api/checkin/manual/documento     DESK
{ "documentNumber": "1234" }
```

Registra con método `MANUAL`, no `DOCUMENTO`. **Cambien ese botón a este endpoint**
— además es el único que puede registrar fuera de horario (ver punto 5).

## 3 — Vuelto pendiente ✅

**3.1 — Confirmado: el vuelto pendiente ya era un estado.** Registrar un movimiento
con `amountReceived > amountApplied` **no entrega nada**. Si dejan de llamar a
"entregar vuelto", la diferencia queda pendiente sin más. No hacía falta cambiar
nada.

**3.2 — `pendingChange`**, el total de vuelto sin devolver de esa persona, viaja en
las dos partes:

```
GET /api/personas/{id}/saldo   → { ..., "pendingChange": 46000, "accountStatus": "VUELTO_PENDIENTE" }
GET /api/cola/admin            → cada fila lleva los dos campos
```

**3.3 — `accountStatus`**, con los tres valores que pidieron y el nombre que
propusieron:

| Valor | Cuándo | Color que pedían |
|---|---|---|
| `DEBE` | saldo negativo | rojo |
| `VUELTO_PENDIENTE` | no debe, y hay dinero suyo en la caja | azul |
| `AL_DIA` | todo lo demás, **incluido el saldo a favor** | — |

El orden es la regla: `DEBE` gana sobre `VUELTO_PENDIENTE`. Si alguien debe dinero y
además tiene un vuelto sin entregar, lo primero es cobrarle. **Dejen de calcularlo**
— la regla vive en `fn_estado_cuenta`.

**3.4 — Convertir el vuelto en abono**, endpoint propio:

```
POST /api/movimientos/{id}/abonar-vuelto      DESK
{ "amount": 46000, "note": "Abona 11 días con el vuelto" }
```

- `{id}` es el **movimiento del cobro**, no el de la persona.
- Acepta parcial: el resto sigue pendiente.
- **No lleva método de pago, y es deliberado: aquí no entra dinero.** Sube el monto
  abonado del mismo movimiento en vez de crear otro, así que el recaudo del día no se
  mueve y los billetes no se cuentan dos veces.
- 400 si el movimiento es un reembolso, si el vuelto ya se entregó, si no hay vuelto
  pendiente o si piden más de lo que queda.

Con eso, los tres botones de la v1: `POST /{id}/vuelto` (entregar), `POST
/{id}/abonar-vuelto` (abonar) y no llamar a nada (dejarlo pendiente).

**3.5 — Entrega parcial:** no existe, como acordaron para v1. `POST /{id}/vuelto`
salda el vuelto entero. Si hace falta parcial, avisen: cambia el diseño.

## 4 — Doble cobro ✅ y la duda de "recaudado hoy"

**4.1 — Dos campos nuevos en cada fila de `GET /api/cola/admin`:**

```jsonc
{ "receivedAmount": 10000, "appliedAmount": 4000 }
```

Son de **esa llegada**, no de la cuenta entera. Con `receivedAmount > 0` la fila
puede decir "Recibido $X" de forma permanente y dejar de ofrecer "Cobrar" como si no
hubiera pasado nada. Los reembolsos de esa llegada ya restan de `appliedAmount`.

**4.2 — Sobre "Recaudado hoy", la respuesta honesta es que hay dos cifras y conviene
que elijan:**

```
GET /api/reportes/recaudo-diario     → lo ABONADO, neto de reembolsos
```

Eso es el dinero que se convirtió en pago o en crédito, con los reembolsos ya
restados. **No son los billetes que entraron en la caja**: quien paga 4.000 con un
billete de 50.000 deja 50.000 en el cajón, pero solo 4.000 cuentan como recaudo
(los otros 46.000 son vuelto pendiente, deuda con esa persona).

Para el encabezado creo que quieren el primero — "cuánto facturó hoy el negocio" — y
el endpoint ya existe. Si lo que quieren es **cuadrar el cajón físico** hace falta
otra cifra (recibido menos vueltos entregados) y se la preparo; díganme cuál es.

## 5 — Fin de sesión y check-ins fuera de horario ✅

**5.1 — La regla está en el servidor**, dentro de la función que resuelve a qué
sesión entra cada llegada, así que **aplica a todos los canales a la vez** (propio,
QR y documento de la terminal):

1. La sesión abierta ahora.
2. Si no hay, **la próxima de hoy si empieza dentro de la hora siguiente**.
3. Si no, el horario vigente en este momento (crea la sesión, como antes).
4. Si tampoco → **rechazo**, `SQLSTATE 75002`, mensaje *"No hay ninguna clase
   programada en este momento"*, que pueden mostrar tal cual.

**Respondiendo a su pregunta: recepción SÍ puede saltársela.** El check-in `MANUAL`
—y solo ese— entra a la última sesión del día en vez de rechazar. Quien atiende
tiene delante a la persona y ve el caso; un canal automático no juzga nada. Por eso
el botón "Agregar manual" debe usar `POST /api/checkin/manual/documento`.

**5.2 — Cierre automático.** Ya existía para las sesiones sueltas sin hora de fin
(cada 15 min, con 2 h de gracia). Las que nacen de un horario **ya traen su hora de
fin desde que se crean**: no hay nada que cerrar en ellas, lo que faltaba era que
alguien dijera que habían terminado — eso es el `status` del punto siguiente.

**5.3 — `sessionStatus` / `status`**, con el reloj del servidor:

| Valor | Cuándo |
|---|---|
| `UPCOMING` | aún no empieza |
| `IN_PROGRESS` | entre inicio y fin, o sin hora de fin |
| `ENDED` | ya pasó su hora de fin |
| `CANCELLED` | cancelada — **gana siempre**, aunque su hora ya pasara |

Viaja en cada fila de `GET /api/cola/admin` (`sessionStatus`) y en cada sesión de
`GET /api/sesiones` (`status`).

**Y eligieron la otra mitad: `GET /api/cola/admin` ahora devuelve por defecto solo
la sesión vigente.** Cuál es la vigente lo decide el servidor: la que está en curso,
si no la próxima, si no la última del día. Dos parámetros para lo demás:

```
GET /api/cola/admin                → solo la sesión vigente  (NUEVO comportamiento)
GET /api/cola/admin?sesionId=42    → una concreta
GET /api/cola/admin?todas=true     → el día entero (lo que devolvía antes)
```

> **Es un cambio de comportamiento en un endpoint que ya usan.** Si alguna pantalla
> necesita el día completo, añadan `?todas=true`.

**5.4 — Cerrar una sesión que ya pasó su hora fin es un no-op.** Devuelve 200 con la
sesión sin tocar, no mueve la hora de fin hacia adelante. Movérsela inventaba una
duración que no fue y, peor, la volvía "vigente" otra vez para el check-in.

**5.5 — Eventos:** el cierre manual y el automático publican `SESSION_CHANGED`, igual
que crear y cancelar.

## 6 — Sesiones por semana con filtro real ✅

```
GET /api/sesiones?desde=2026-09-14&hasta=2026-09-20&estado=activas
```

`estado` acepta `activas` (por defecto), `canceladas` o `todas`. Un valor distinto
responde 400 en vez de devolver todo en silencio.

El rango sigue siendo opcional y con la misma regla de siempre: **o los dos o
ninguno**. Y cada sesión trae su `status` (punto 5.3), así que el botón "Finalizar"
puede decidirse sin comparar horas.

## 7 — Rol profesor ✅

**7.1 — Sus sesiones:**

```
GET /api/sesiones/mias?desde=&hasta=      el profesor sale del TOKEN
```

No lleva el id en la ruta a propósito: si viniera en la URL, cualquier profesor
podría ver la agenda de otro cambiando el número. Paginado y con el mismo `status`.

> Sobre `totalCheckins` / `totalAttended`: esos ya los da
> `GET /api/sesiones/resumenes?desde=&hasta=`, que existe desde antes. No los
> dupliqué en `/mias` para no tener dos sitios que cuenten lo mismo; si les estorba
> hacer dos llamadas, díganme y los añado.

**7.2 — Finalizar las suyas:** `PUT /api/sesiones/{id}/cerrar` acepta a un PROFESOR
**solo si la sesión es suya**, y responde **403** si no. ADMIN y RECEPCIÓN cierran
cualquiera.

**7.3 — Quién asistió:**

```
GET /api/sesiones/{id}/asistentes      STAFF (profesor, admin y recepción)
→ [ { "personId": 17, "fullName": "Ana Pérez",
      "arrivalTime": "...", "attended": true,
      "markedAt": "...", "amountCharged": 4000 } ]
```

`/resumen` solo da totales, por eso hacía falta este. Incluye a quien llegó y **no**
fue confirmado (`attended: false`), que es información que el profesor necesita.

## 8 — Observaciones de salud — revisado, el servidor está bien

**No puedo ejecutar las pruebas que piden: no tengo acceso a una base de datos desde
aquí.** Lo que sí hice fue seguir el dato por todo el código, que descarta la
sospecha:

| Paso | Estado |
|---|---|
| Se guarda en `persona.observaciones_salud` | ✅ |
| `vista_perfil_persona` la incluye | ✅ |
| `GET /{id}/perfil` la devuelve (`healthObservations`) | ✅ |
| `GET /api/personas/{id}` la devuelve | ✅ |
| `PUT /{id}/mis-datos` **no** la toca | ✅ verificado en el mapper generado |
| `PUT /api/personas/{id}` de mostrador **no** la toca | ✅ igual |
| Contactos de emergencia **no** la tocan | ✅ otro servicio, otra tabla |
| Guardarla de nuevo con contenido no revoca nada | ✅ el consentimiento solo se graba en la transición |
| Por el camino del registro | ✅ mismo servicio, misma transacción |

**Solo dos cosas la borran**, las dos a propósito: enviar el campo vacío (`null` o
`""`) en `PUT /{id}/observaciones-salud`, o revocar el consentimiento de salud.

La primera es exactamente el defecto de pantalla que ya corrigieron — abrir en una
vista que no mostraba lo guardado y pulsar Guardar. **Con eso, el punto se cierra por
nuestra parte**, pero confírmenlo en la próxima prueba y si vuelve a fallar, avisen
con el `PUT` y el `GET` que hicieron.

## 9 — Despliegue — es configuración, no código ✅

Buenas noticias: **no hace falta cambiar nada**, las cuatro cosas ya son variables.

**1 y 2 — CORS y WebSocket: una sola variable cubre los dos.**

```
CORS_ALLOWED_ORIGINS=https://<proyecto>.pages.dev
```

Es la misma propiedad para REST y para el handshake de `/ws`, a propósito — tenerlas
en dos sitios es la forma segura de que se desincronicen. Acepta **lista separada por
comas** y **patrones con comodín**, así que para los deploys de vista previa de
Cloudflare (cada rama crea su subdominio) sirve:

```
CORS_ALLOWED_ORIGINS=https://<proyecto>.pages.dev,https://*.<proyecto>.pages.dev
```

Sobre `wss`: lo da el proxy TLS del backend, no la aplicación. Si el backend se sirve
por `https`, el WebSocket va por `wss` sin tocar nada. **Si el backend queda en
`http`, el navegador bloqueará la conexión desde una página `https`** — eso sí hay
que resolverlo en el despliegue.

**3 — Enlaces de los correos:**

```
FRONTEND_URL=https://<proyecto>.pages.dev
```

Esa variable define la base de los tres correos (bienvenida, restablecer contraseña y
confirmar cambio de correo). Sin ella quedan apuntando a `localhost:5173`.

**4 — La URL pública del backend** depende de dónde se despliegue; todavía no está
decidido (ver el punto 1 de `pendiente-implementar-sistema.md`). En cuanto se decida,
es el valor de su `VITE_API_URL`.

Y el recordatorio de siempre para ese despliegue: `SEED_ENABLED=false` y
`SEED_HISTORICAL_ENABLED=false` si es un entorno real; si es el de pruebas, al revés
— con el histórico en `true` tendrán datos con los que probar.

---

# Bloque 5 — Ronda 2 del 20/21-sep

Respuesta punto por punto a `docs/pendientes-back-20-09-ronda2.md`. **Los 7 están
atendidos.** Los parches provisionales del punto 10 de `mejoras-v2.md` se pueden
quitar todos.

## 1 — Cuánto debe pagar hoy ✅ `amountDue` y `coveredByCredit`

Dos campos nuevos en cada fila de `GET /api/cola/admin` y en la respuesta de
`/api/checkin/propio`:

```jsonc
{ "amountDue": 2000, "coveredByCredit": false }
```

- **`amountDue`** — lo que hay que cobrarle **ahora**, con su deuda anterior y su
  saldo a favor ya descontados. `0` si no hay nada que cobrar.
- **`coveredByCredit`** — `true` cuando la sesión de hoy ya está pagada con días
  adelantados. Puede pasar directo.

**Su parche era correcto**, y ahora vive donde debe. La regla es literalmente la que
habían deducido:

```
si la asistencia ya se confirmó   amountDue = max(0, -saldo)
si todavía no                     amountDue = max(0, tarifa - saldo)
```

**Y confirmado: el monto aplicado paga primero la deuda anterior y luego la tarifa
de hoy.** Pero no porque haya un reparto explícito — es que **todo entra al mismo
saldo**. No existe una imputación contra partidas: se abona a la cuenta, y la
asistencia cobra la tarifa de la cuenta. El orden se cumple por construcción, no
porque alguien lo programara.

> `pendingChange` sigue igual. Y tienen razón en separarlo: **no es deuda de la
> persona, es deuda con la persona.** "Vuelto por entregar" es el nombre correcto.

## 2 — Los seis casos de cobro

**Aviso primero: no pude ejecutarlos.** No tengo base de datos desde aquí. Lo que
sigue es el resultado de aplicar la fórmula al modelo de saldo, caso por caso — es
razonamiento sobre el código, no una prueba real. Con tarifa de 5.000:

| Punto de partida | Se recibe | `balance` | `pendingChange` | `accountStatus` | `amountDue` |
|---|---|---|---|---|---|
| Sin saldo | 5.000 | 0 | 0 | `AL_DIA` | 0 |
| Sin saldo | 7.000, se deja pendiente | 0 | 2.000 | `VUELTO_PENDIENTE` | 0 |
| Debe 5.000 | 10.000 | 0 | 0 | `AL_DIA` | 0 |
| Debe 5.000 | 5.000 | 0 | 0 | `AL_DIA` | **5.000** |
| Debe 2.000 | 5.000 | 3.000 → 0 al confirmar | 0 | `AL_DIA` | **2.000** |
| 1 día adelantado | 0 | 5.000 → 0 al confirmar | 0 | `AL_DIA` | 0, `coveredByCredit: true` |

**Los seis coinciden con lo que esperaban.** Un matiz del cuarto: tras recibir 5.000
el `balance` queda en 0 y el `accountStatus` en `AL_DIA`, pero `amountDue` sigue
siendo 5.000 — la deuda se saldó y la clase de hoy aún no se ha pagado. Son dos
preguntas distintas y por eso hacen falta los dos campos.

**Cuando prueben, si alguno da otra cosa, avísenme con las cuatro cifras.**

## 3 — Qué muestra la cola al terminar la sesión ✅

**Corregido — y tenían razón.** En la ronda anterior programé que, si no había
sesión en curso ni próxima, devolviera la última que hubiera terminado, para no
dejar la pantalla vacía. **Eso era peor**, exactamente como vieron: la lista seguía
mostrando a la gente de la clase cerrada y se podía seguir cobrando sobre esas filas.

Ahora `GET /api/cola/admin` sin parámetros devuelve **la sesión en curso, si no la
próxima del día, y `[]` si no hay ninguna viva**. Nunca las de una sesión cerrada.
(`?todas=true` y `?sesionId=` siguen igual, para el histórico.)

**Y el resumen que pedían, en endpoint propio:**

```
GET /api/cola/resumen      STAFF
→ {
    "currentSessionId": 42, "currentStartTime": "...", "currentEndTime": "...",
    "nextSessionId": 43,    "nextStartTime": "2026-09-21T20:00:00-05:00",
    "hasSessionInProgress": true,
    "todaySessions": 2
  }
```

Sale de la tabla de sesiones, no de las filas, **a propósito**: una clase que todavía
no tiene a nadie en la cola sigue siendo la próxima, y es justo lo que hay que
anunciar cuando la anterior acaba de cerrar. Con esto, el aviso "no hay clase en
curso" puede decir a qué hora empieza la siguiente.

**Sobre la deuda en la sesión siguiente: ya funciona así.** `accountStatus`,
`balance` y `amountDue` son de la **persona**, no de la sesión. Quien debe llega
mañana y llega debiendo.

## 4 — Generación anticipada — **una semana, no cuatro**

**Aquí hay que corregir la propuesta.** Pidieron cuatro semanas; **la decisión de
producto es una sola semana**, y el horizonte de 7 días que ya existe es
intencionado, no un descuido. La razón está escrita desde el 11-sep: una ventana
corta es suficiente para que la pantalla no se vea vacía, y evita que reconfigurar un
horario obligue a arrastrar un mes de sesiones ya creadas.

**Eso explica exactamente lo que vieron:** el barrido genera **hoy + 6 días**, así
que cuánto de "la semana que viene" existe depende del día en que miren. Un lunes no
verán nada del lunes siguiente; un viernes verán media semana. **No es un fallo** —
pero si la pantalla de Sesiones va a navegar por semanas, conviene que sepan que más
allá de 7 días no habrá nada, y quizá decirlo en pantalla ("la programación se genera
con una semana de antelación").

Lo demás del punto:

2. **`POST /api/horarios/generar-sesiones` ya es idempotente** y responde cuántas
   creó (`"3 sesión(es) creada(s)."` o `"No hacía falta crear ninguna..."`). El botón
   "Regenerar programación" puede usarlo tal cual.
3. **`estado=activas` SÍ incluye las futuras.** "Activas" significa **no canceladas**,
   nada más — no tiene relación con si ya pasaron o si tienen check-ins. Si no ven
   nada de la semana siguiente es por el horizonte de 7 días, no por el filtro.
4. **Cuando cambia el horario, ahora se reprograman las sesiones futuras que no
   tienen a nadie en la cola** (decisión del 21-sep). Antes no se tocaba ninguna y
   quedaban con la hora vieja.

   **Las que ya tienen check-ins no se tocan**, y esa es la línea: mover la hora de
   una clase a la que alguien ya se apuntó le cambia la cita sin decírselo. Tampoco
   las de hoy ni las pasadas — reescribir la hora de una clase que ya ocurrió sería
   falsear el histórico. Si además cambia el **día de la semana**, las sesiones que
   quedan huérfanas se dejan como están en vez de moverlas de fecha.

## 5 — Las tres definiciones de recaudo ✅ escritas, y una con nombre propio

```
GET /api/reportes/recaudo-hoy      DESK
→ { "collectedToday": 40000 }
```

**Es la que recepción necesita.** Y aquí están las tres, por escrito:

| Cifra | Qué es | Cuándo cuenta |
|---|---|---|
| **`collectedToday`** *(nueva)* | Lo **abonado** hoy, neto de reembolsos | La fecha del **pago** |
| `totals.day.revenue` (dashboard) | Lo **devengado**: lo que generaron las clases de hoy | La fecha de la **clase** |
| Recaudo por sesión | Lo devengado, repartido por sesión | La fecha de la **clase** |

La diferencia no es cosmética: un abono adelantado el lunes para la clase del
miércoles cuenta en `collectedToday` el **lunes** y en el dashboard el **miércoles**.

**Y la decisión que pedían anotada** — un cobro de 50.000 con 45.000 de vuelto cuenta
**5.000**, no 50.000. Lo que la persona entregó de más sigue siendo suyo hasta que se
le devuelva o lo abone, así que no es recaudo; contar lo recibido haría que un billete
grande multiplicara la cifra del día. Los reembolsos restan (devolver dinero baja la
caja).

> Si en algún momento hace falta **cuadrar el efectivo físico del cajón** — que es
> otra pregunta— hace falta otra cifra (recibido menos vueltos entregados). Díganme y
> la añado como `cashInDrawer`; no la inventé por no meter un número más que nadie
> pidió.

## 6 — Detalles de forma ✅ los tres

**`GET /api/checkin/propio`** trae ahora `paymentStatus` y `amountDue`:

| `paymentStatus` | Qué decirle al miembro |
|---|---|
| `PAGADA_POR_ADELANTADO` | "Tu sesión ya está paga, puedes pasar directo" |
| `PAGADA` | Asistencia confirmada y sin deuda |
| `PENDIENTE` | Tiene que pagar `amountDue` |

No hay que deducirlo de `creditDays`.

**`GET /api/sesiones` y `/mias`** traen `teacherName` ya resuelto, `totalCheckins` y
`totalAttended`. Se acabó cruzar tres respuestas — y el retraso al recargar era eso.
Van resueltos en la misma consulta, sin una petición por sesión.

**`canClose` y `canCancel`** en cada sesión, según **quién pregunta**:

- `canClose`: mostrador siempre; profesor **solo en las suyas**. Y nunca en una
  cancelada ni en una que ya terminó, porque ahí cerrar no hace nada.
- `canCancel`: cualquiera que opere la clase, mientras no esté ya cancelada.

Replican exactamente lo que hacen los endpoints: un botón que se pinta y luego
responde 403 es peor que no pintarlo.

## 7 — Si el botón sigue apareciendo al recargar

**No puedo darles la respuesta cruda: no tengo base de datos desde aquí.** Lo que sí
puedo decirles es qué mirar, porque hay un caso legítimo en que `GET
/api/checkin/propio` devuelve **204 aunque la persona sí marchó llegada**:

> Lee de la cola del día, que **excluye las sesiones canceladas**. Si la sesión a la
> que entró se cancela después, su llegada deja de existir para esa consulta — y es
> correcto: esa clase no se va a dar.

Así que si vuelve a pasar, lo primero es mirar si la sesión sigue viva. Lo demás a
comprobar, en orden:

1. El `POST` devolvió **201 o 200** (las dos son éxito) — si devolvió 400/403, no hay
   llegada que encontrar.
2. `sessionStatus` en la respuesta del `POST`: si es `ENDED`, el `GET` posterior sí
   la devuelve, pero el `POST` siguiente creará otra — es el caso de dos clases en un
   día.
3. Que sea **el mismo usuario**: la persona sale del token, así que un token de otra
   cuenta da 204 con toda la razón.

Con el estado HTTP y el cuerpo de las dos llamadas seguidas lo depuro con ustedes.

---

# Bloque 6 — Ronda del 21-sep (la del deploy)

Respuesta a `docs/pendientes-back-21-09.md`. **Los 4 puntos están implementados.**

## 1 — Señales en tiempo real ✅

### 1A — La llegada propia que no se veía: **era un bug mío de la ronda del 20-sep**

Encontrado y corregido. Cuando cambié `POST /api/checkin/propio` para que fuera
repetible, le puse una rama "ya tenías llegada, te devuelvo la existente" — y **esa
rama salía sin publicar ninguna señal**. Su diagnóstico era exacto: la lectura
funcionaba, lo que no llegaba era el aviso.

Eso explica el paso 5 del reporte al pie de la letra: la segunda llegada del mismo
miembro (o la primera, si venía de una sesión anterior del día que aún no había
terminado) entraba por ahí y recepción no se enteraba hasta que alguien agregaba a
otra persona a mano.

**Ahora se publica siempre**, también cuando se devuelve la llegada existente.
Reavisar de más no cuesta nada — ustedes solo releen— y no avisar deja la pantalla
mintiendo.

> Sobre el **AFTER_COMMIT**: ya era así y sigue siéndolo, en las tres vías. El evento
> se publica dentro de la transacción pero el broadcaster lo escucha en
> `AFTER_COMMIT`, así que cuando les llega la señal la llegada ya está confirmada en
> la base. Nunca van a releer y no verla.

### 1B — Sesión que empieza o termina por el reloj ✅

**Había un hueco real y no lo había visto:** todos los avisos de cola nacían de que
alguien escribiera algo. El estado de una sesión cambia **solo con la hora**, sin que
nadie toque nada, y ahí no había ningún evento que publicar.

Scheduler nuevo, **cada minuto**: detecta las sesiones de hoy que empezaron o
terminaron en el último minuto y publica `SESSION_CHANGED` con su `sessionId`.

La ventana que mira hacia atrás (75 s) es algo más ancha que el intervalo a
propósito: si una pasada se retrasa, un margen justo perdería transiciones. Puede
llegarles el mismo aviso dos veces — es inofensivo, releen y ya.

> **La precisión es de hasta ~1 minuto, no al segundo.** Con una sesión a las 11:40,
> la señal llega entre las 11:40:00 y las 11:41:00, no exactamente a las 11:40:00.
> Si necesitan el segundo exacto, díganmelo — pero para pintar o esconder un botón
> creo que sobra, y bajar el intervalo es una consulta por minuto más.

## 2 — Confirmar la asistencia cuando ya está cubierta ✅

Al registrar una llegada **por las tres vías**, si la persona no debe nada por hoy
porque su crédito cubre la sesión, el servidor **confirma la asistencia y descuenta
la sesión en ese mismo momento**. La fila nace con `attended: true` y ya no aparece
en "Sin atender".

Los cinco casos de su tabla salen como esperaban:

| Punto de partida | Resultado |
|---|---|
| Crédito 5 (25.000 a favor) | `attended: true`, 20.000 a favor, `AL_DIA`, `amountCharged` = tarifa |
| Crédito 1 (5.000 a favor) | `attended: true`, saldo 0, `AL_DIA` |
| Crédito 0, sin deuda | **No** se confirma — `amountDue` = tarifa |
| Abonó 1.000 hoy | **No** se confirma — `amountDue` = tarifa − 1.000 |
| Debe 2.000 | **No** se confirma — `amountDue` = tarifa + 2.000 |

La condición es **exactamente** `coveredByCredit`, que ya existía: cierto solo si la
asistencia aún no está confirmada **y** lo que hay que cobrar es cero. Con un abono
parcial o con deuda, la fila se queda para el mostrador — ahí sí hay una decisión
que tomar y dinero que recibir.

**Y los cuatro casos borde, sin código extra:**

- **Idempotencia** — la vista pone `coveredByCredit` en `false` en cuanto existe la
  asistencia, así que marcar dos veces no puede descontar dos veces. No es una
  guarda que alguien recuerde poner: es la forma del dato.
- **Dos sesiones el mismo día** — cada llegada es su propia fila y evaluúa el crédito
  en ese momento: crédito 5 → 3 al final del día.
- **Deshacer** — el endpoint de anular asistencia devuelve la sesión al crédito, igual
  que con una confirmación manual. No hay diferencia entre las dos.
- **Sesión cancelada** — no aparece en la vista, así que no descuenta nada.

Un detalle para la pantalla de historial: estas confirmaciones quedan **a nombre del
usuario SISTEMA**, no de una persona del mostrador. No lo confirmó nadie, y
atribuírselo a alguien real sería falso.

## 3 y 4 — Las dos cifras y la sesión vigente ✅ en una sola respuesta

```
GET /api/cola/resumen      ADMIN, RECEPCIÓN y PROFESOR
→ {
    "session": {
      "id": 42, "startTime": "...", "endTime": "...",
      "status": "IN_PROGRESS", "teacherUserId": 7, "teacherName": "Carlos Muñoz",
      "totalCheckins": 12, "totalAttended": 8,
      "canClose": null, "canCancel": null
    },
    "collectedThisSession": 10000,
    "collectedToday": 20000
  }
→ "session": null   si hoy no hay ninguna sesión
```

**⚠️ Cambié la forma de este endpoint.** Lo creé ayer con campos sueltos
(`currentSessionId`, `nextStartTime`…) y hoy pasa a llevar el objeto `session`
completo, que es lo que pidieron. **Si ya lo habían conectado, hay que ajustarlo.**

**La sesión viene completa y sale de la tabla, no de las filas de la cola.** Eso
resuelve el 4B: una clase en curso con cero llegadas ya tiene id, hora de fin y
estado. Y resuelve el 4A: **dejen de elegir la sesión de un listado** para el botón
"Finalizar" — usen `session.id` de aquí, que se calcula con el reloj del servidor en
el momento de preguntar.

> `canClose` y `canCancel` vienen en `null` en esta respuesta (no en `/api/sesiones`,
> donde sí se calculan). Si los necesitan aquí, se añade en dos minutos.

**"Vigente" es: la que está en curso; si no hay, la próxima de hoy; si no hay, la
última de hoy.** La tercera rama es distinta de lo que hace `GET /api/cola/admin`, y
a propósito: la cola responde *"a quién atiendo"* y cuando la última clase terminó
la respuesta correcta es "a nadie" (`[]`); esto responde *"de qué clase estamos
hablando"*, y ahí la última del día sigue siendo útil.

### Las dos definiciones, por escrito

| Cifra | Qué es | Por qué fecha cuenta |
|---|---|---|
| **`collectedThisSession`** | Lo cobrado en las **asistencias confirmadas de esa sesión** | La de la **clase** (devengado) |
| **`collectedToday`** | Todo lo **abonado hoy**, adelantos incluidos, **restando reembolsos** | La del **pago** (caja) |

**Con su caso de prueba dan 10.000 y 20.000**, que es lo que proponían:

- `collectedThisSession` = A 5.000 + B 5.000 = **10.000**. El adelanto de A no cuenta
  aquí (es para otra clase) y el vuelto de B tampoco (nunca se abonó).
- `collectedToday` = A 10.000 + B 5.000 + C 0 (abonó 5.000 y se le reembolsaron
  5.000) + D 5.000 = **20.000**.

Un profesor recibe la sesión y las **dos cifras en `null`** — van en null y no se
omite el campo, para que puedan distinguir "no puedo verlo" de "es cero".

> `GET /api/reportes/recaudo-hoy` sigue existiendo y devuelve la misma cifra que
> `collectedToday`. Si ya usan el resumen, ese no hace falta.

---

# Resumen de lo que hay que tocar en el frontend

| Qué | Dónde |
|---|---|
| Casilla de formato físico firmado | `PersonFormModal.jsx` (si no está ya) |
| Cambiar el endpoint de guardado | "Mi Perfil" → `PUT /api/personas/{id}/mis-datos` |
| Quitar el candado por rol del botón guardar | "Mi Perfil" |
| Botón "Regenerar sesiones" | Pantalla de Horarios |
| Tope del modal de reembolso | usar `refundable` tal cual; ya no hace falta recortarlo contra `saldo` |
| Formulario de salud y contactos en el registro | las cuatro pantallas de alta |
| Selector de métodos | `GET /api/metodos-pago?activo=true`, y quitar el filtro del cliente |
| Manejar `SESSION_CHANGED` | el `switch` de eventos de cola |
| **`GET /api/checkin/propio`** al cargar | pantalla del miembro (sobrevive al F5) |
| **`?todas=true`** si alguna pantalla necesita el día entero | `GET /api/cola/admin` cambió de comportamiento |
| **Dejar de calcular el estado de cuenta** | usar `accountStatus` |
| **Dejar de comparar horas** | usar `sessionStatus` / `status` |
| **"Agregar manual"** | `POST /api/checkin/manual/documento` |
| **Tercer botón del vuelto** | `POST /api/movimientos/{id}/abonar-vuelto` |
| **"Recibido $X"** en la fila | `receivedAmount` / `appliedAmount` |
| **Pantalla del profesor** | `/api/sesiones/mias` y `/api/sesiones/{id}/asistentes` |
| `checkinMethod: SELF` → "App / miembro" | check-in de recepción |
| **Quitar el cálculo de `amountDue`** | usar el campo; la regla ya no vive en el cliente |
| **Quitar el apartado de filas `ENDED`** | la cola ya no las devuelve |
| **`GET /api/cola/resumen`** | encabezado: clase en curso y próxima |
| **`GET /api/reportes/recaudo-hoy`** | "Recaudo de hoy", con definición única |
| **`paymentStatus`** en la pantalla del miembro | "puedes pasar directo" |
| **`teacherName` / `totalCheckins` / `totalAttended`** | Sesiones: una sola respuesta |
| **`canClose` / `canCancel`** | Sesiones: pintar los botones |
| Avisar que la programación es de **1 semana** | pantalla de Sesiones |
| **`GET /api/cola/resumen` cambió de forma** | ahora lleva `session` completa — ajustar si ya estaba conectado |
| **"Finalizar sesión" → `session.id` del resumen** | dejar de elegirla de un listado |
| **Quitar el relleno de 10 s** si estorba | las señales ya llegan solas |
| Nada | la confirmación automática por crédito no necesita cambios en el cliente |

---

# Bloque 7 — Ronda del 21-sep (noche): cola de la terminal y cuentas de producción

Responde al mensaje del equipo de front sobre la pantalla de la TV
(`/lobby/queue`) y prepara las cuentas para el deploy.

## 7.1 `GET /api/cola/espera` — ahora filtra por sesión vigente

**Antes:** devolvía todas las llegadas del día sin asistencia confirmada, de
todas las sesiones. La TV mezclaba a quien espera ahora con los de una clase ya
terminada.

**Ahora:** aplica la misma regla de sesión vigente que `/api/cola/admin`:

1. La sesión en curso.
2. Si no hay ninguna en curso, la próxima de hoy.
3. Si no hay ninguna viva, `[]`.

**El contrato no cambia.** La respuesta sigue siendo la misma estructura:

```jsonc
[
  {
    "checkinId": 42,
    "sessionId": 5,
    "firstName": "Ana",
    "lastName": "Pérez",
    "arrivalTime": "2026-09-21T18:05:00-05:00",
    "position": 1
  }
]
```

Sin montos, sin saldos, sin cambios en los campos. Lo único que cambia es **qué
filas** aparecen.

> **Acción front:** ninguna. Si la TV ya consumía este endpoint, ahora recibirá
> solo las filas de la sesión correcta sin tocar nada.

## 7.2 `GET /api/cola/prepagada` — ahora filtra por sesión vigente e incluye confirmados automáticos

Dos correcciones en uno:

### Filtro por sesión vigente
Misma regla que `/espera` (en curso → próxima → `[]`). Antes devolvía prepagados
de todas las sesiones del día.

### Confirmados automáticos por crédito
**Antes:** quien llegaba con crédito suficiente era confirmado automáticamente
por el sistema (`confirmIfCoveredByCredit`), y en ese instante `a.id` dejaba de
ser NULL → desaparecía de `vista_fila_prepagada`. La sección "Pasan directo"
quedaba vacía aunque la persona acabara de llegar.

**Ahora:** el endpoint complementa la vista original con las filas de
`vista_fila_hoy` que cumplen:

- Asistencia confirmada (`attended = true`)
- `amountDue = 0` (la sesión estaba cubierta por crédito)
- Hora de llegada dentro de la ventana `paymentVisibilityMinutes` (leída de
  `configuracion_cola`)

**El contrato no cambia:**

```jsonc
[
  {
    "checkinId": 43,
    "firstName": "Santiago",
    "lastName": "Herrera",
    "arrivalTime": "2026-09-21T18:03:00-05:00"
  }
]
```

> **Acción front:** ninguna. La respuesta tiene la misma forma. Ahora aparecerán
> filas que antes no llegaban.

## 7.3 `GET /api/cola/admin` — sin cambios

Sigue requiriendo `STAFF` (ADMIN, TEACHER, RECEPTIONIST). PSR_TERMINAL recibe
**403**, que es el comportamiento esperado: la TV no debe ver montos ni saldos.

El fallback del front (`/espera` + `/prepagada`) ahora funciona correctamente con
los dos fixes de arriba.

## 7.4 Cuentas de producción

Se creó `ProductionAccountsSeeder`: al arrancar la app **siempre** (sin bandera
condicional) se crean estas cuentas si no existen:

| Correo | Rol | Uso |
|---|---|---|
| `adminpsraerobicos@gmail.com` | ADMIN | Panel de administración |
| `terminal1@psraerobicos.com` | PSR_TERMINAL | Equipo físico de entrada 1 |
| `terminal2@psraerobicos.com` | PSR_TERMINAL | Equipo físico de entrada 2 |
| `terminal3@psraerobicos.com` | PSR_TERMINAL | Equipo físico de entrada 3 |

Eran cinco terminales hasta el 28-09-2026; PSR opera con tres. Como el seeder es
idempotente y **no borra**, `terminal4` y `terminal5` siguen existiendo en
cualquier base donde ya hubiera corrido: hay que desactivarlas a mano.

Ninguna de estas cuatro tiene persona asociada — `fullName` vendrá `null` en el
login. El front ya maneja eso: `PersonNames.fullName()` devuelve `null` si no hay
persona.

### Cuentas compartidas de mostrador

El mismo seeder crea dos **personas** (no cuentas técnicas: estas sí llevan
`persona`, porque tienen que poder entrar a la fila y que se les marque
asistencia):

| Documento | Nombre | Para qué |
|---|---|---|
| `INVITADO` | Invitado PSR | Quien entra sin cuenta propia y no hay tiempo de darlo de alta |
| `MENOR` | Menor De Edad | Lo mismo, pero dejando constancia de que quien entró era menor, sin guardar ningún dato suyo |

Aparecen en el buscador de personas como cualquier otra — tiene que ser así, el
mostrador necesita encontrarlas. **No pueden iniciar sesión:** su `password_hash`
no es bcrypt, así que ninguna contraseña hace match.

> ⚠️ **Límite que el front debe reflejar:** solo cabe **una llegada por cuenta y
> por sesión**. Si ya hay un invitado registrado en la clase en curso, el
> check-in del segundo devuelve la misma llegada en vez de crear otra, y no
> genera un segundo cobro. La pantalla no debería dar a entender que quedó
> registrado un invitado nuevo.

> **Acción front para la TV:** configurar el login de la terminal con el correo
> `terminalN@psraerobicos.com` y su contraseña correspondiente.

## Checklist actualizado

| Qué hacer | Dónde |
|---|---|
| Nada en `/espera` ni `/prepagada` | la TV recibe las filas correctas sin cambios de contrato |
| Configurar credenciales de terminal | login de la TV con `terminalN@psraerobicos.com` |
| Verificar que "Pasan directo" muestre a quien llega con crédito | pantalla de TV |
