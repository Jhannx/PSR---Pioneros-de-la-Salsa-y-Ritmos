# Auditoría del backend — mapa de flujos, responsabilidades y decisiones

Fecha: 2026-09-23. Cubre el código tal como está en `main` (último commit: 57a45d1, 22-sep).

## Para qué sirve este documento

Para **auditar el backend antes de la v1 sin tener que leerlo entero**: qué hace cada apartado, por
qué clases pasa, en qué orden, y qué se decidió y por qué. Con eso se puede ir clase por clase
diciendo *"esta implementación no me gusta, se cambia"* o *"me gusta, se queda"*.

Cada apartado trae cinco cosas:

| | |
|---|---|
| **Qué resuelve** | el problema real, en una frase |
| **Por dónde entra** | los endpoints, con su permiso |
| **Qué clases** | rutas navegables, en orden de lectura |
| **Cómo fluye** | el recorrido, paso a paso |
| **Decisiones / qué decidir** | lo que se eligió a propósito, y lo que queda abierto para la v1 |

Los problemas conocidos están al final (§14) y **no son el foco**: están para tenerlos presentes
mientras se lee, no para dirigir la lectura.

**Marcas:** ✅ decidido y funcionando · ⚠️ hay una decisión que tomar antes de la v1 · 🔴 hay que
limpiarlo antes de producción.

---

## 0. Cómo está montado — lo transversal

Seis convenciones que se repiten en todo el proyecto. Si se entienden estas seis, el resto del
código se lee solo.

### 0.1 Las capas

```
  Controller          permisos (@PreAuthorize), validación del DTO, id del usuario desde el JWT
      ↓
  Service (I + impl)  reglas de aplicación, @Transactional, publica eventos
      ↓
  Repository          JPA para lo simple | *ProcedureRepository (JdbcTemplate) para los CALL
      ↓
  PostgreSQL          procedimientos, funciones, vistas y triggers: aquí vive el negocio duro
```

Cada módulo repite la misma estructura de carpetas: `controller/ dto/{request,response}/ entity/
enums/ mapper/ repository/ service/ view/`. Los servicios tienen interfaz (`IPersonService`) e
implementación; las excepciones son cuatro propias (`ResourceNotFound`, `Duplicate`, `InvalidData`,
`Conflict`) más dos de dominio (`DeactivatedPerson`, `NoActiveSession`).

### 0.2 Las reglas de negocio viven en la base, no en Java

Esta es **la decisión estructural del proyecto** y conviene tenerla clara antes de juzgar nada:
cobrar, resolver a qué sesión entra quien llega, calcular saldo, decidir cuánto se paga hoy y dejar
rastro de auditoría **se hacen en PostgreSQL** (procedimientos, funciones, triggers, vistas). Java
coordina, autoriza y traduce.

- **A favor:** una sola definición de cada fórmula, imposible de saltarse desde otro cliente, y
  atómica por construcción. Los tres fallos de dinero más caros del proyecto salieron justamente de
  tener la fórmula escrita *dos* veces (una en SQL y otra en el front o en un repositorio).
- **En contra:** la lógica no se puede testear con JUnit, no aparece en el IDE al refactorizar, y
  exige saber plpgsql para tocarla. Cambiar una regla es una migración repetible, no un commit de
  Java.
- ⚠️ **Qué decidir:** nada para la v1 — está hecho y funciona. Pero conviene decidir *si esto se
  mantiene* como criterio para la v2, porque condiciona a quién se puede meter a mantener el código.

### 0.3 El «patrón H»: CALL + relectura

Los procedimientos devuelven como mucho un id por parámetro `OUT`. Todo lo demás (nombre, posición
en la cola, monto cobrado) se lee **después del CALL, dentro de la misma transacción**, normalmente
desde una vista. Está anotado como "Patrón H" en los comentarios del código.

Se ve en `CheckinService.register`, `AttendanceService.confirm`, `AccountMovementService`
(`registerDeposit`, `registerRefund`, `deliverChange`) y `SessionService.createManual`.

### 0.4 Enums: inglés en Java, español en la base

Las constantes Java están en inglés (`DOCUMENT`, `ADMIN`, `DEPOSIT`) y los valores de Postgres en
español (`DOCUMENTO`, `ADMIN`, `ABONO`). Traduce un `AttributeConverter` por enum
(`CheckinMethodConverter`, `AuditableTableConverter`, `MovementTypeConverter`,
`RegistrationSourceConverter`, `ConsentTypeConverter`, `ConsentMediumConverter`, `TokenTypeConverter`).

**La regla que hay que respetar siempre:** el converter **solo actúa entre una entidad y su
columna**. Si un valor cruza de una vista a un DTO tiene que estar tipado como enum *en los dos
lados*; un `String` en medio significa que MapStruct va a generar `valueOf("DOCUMENTO")` y reventar.
Este error se pagó tres veces (ver §14).

### 0.5 Permisos: un único sitio donde están escritos

`security/Authorities.java` define las expresiones SpEL con nombre, y los controllers solo las citan:

| Constante | Quién entra | Para qué |
|---|---|---|
| `ADMIN` | ADMIN | dinero de catálogo, roles, auditoría, config |
| `DESK` | ADMIN, RECEPCIONISTA | mostrador: cobrar, registrar, corregir |
| `STAFF` | ADMIN, TEACHER, RECEPCIONISTA | lectura del día a día |
| `CLASS_OPS` | ADMIN, TEACHER, RECEPCIONISTA | operar la clase: abrir, cerrar, check-in |
| `MEMBER` | MEMBER | lo que actúa sobre uno mismo |
| `PERSON_LOOKUP` / `TERMINAL_QUEUE` | + PSR_TERMINAL | la terminal del lobby |
| `OR_SELF` / `OR_SELF_PERSON_ID` / `OR_OWNS_CONTACT` | sufijos | «…o si es su propio dato» |

Los sufijos resuelven contra `OwnershipGuard` (registrado como bean `"ownership"`), que compara el
id de la ruta con la persona de la cuenta. `OR_OWNS_CONTACT` existe porque en `/api/contactos-emergencia/{id}`
el id es de *contacto*, no de persona: compararlo con `isSelf` dejaría a un miembro editar el
contacto de otro cuando los números coinciden por casualidad.

**Multirol:** el token lleva **todas** las autoridades a la vez, no se alterna entre roles. Quien es
ADMIN y PROFESOR pasa por las dos puertas en la misma sesión.

### 0.6 Errores: un solo traductor

`shared/exception/GlobalExceptionHandler.java` (21 handlers) convierte todo a
`{status, code, message}`. Lo importante:

- `P0001` (cualquier `RAISE EXCEPTION` de plpgsql sin código propio) → **400 con el texto del RAISE**.
  Es lo que hace que las reglas escritas en SQL lleguen al usuario en vez de morir como "error
  interno". **Contrapartida: el mensaje del RAISE se le enseña al usuario**; si habla de tablas o
  parámetros hay que marcarlo con `USING ERRCODE = '75000'`.
- `75001` → 409 `PERSONA_DESACTIVADA` · `75002` → 409 `SIN_SESION_ACTIVA` · `75000` → 500 (fallo
  nuestro, se registra entero y no se cuenta).
- Los CHECK del esquema se identifican **por nombre de constraint**, nunca por el texto de Postgres
  (que cambia con el locale y la versión).

---

## 1. Seguridad y autenticación

**Qué resuelve.** Quién eres (JWT), qué puedes hacer (roles) y sobre quién (ownership), sin sesión en
el servidor.

### Por dónde entra

| Endpoint | Permiso | Qué hace |
|---|---|---|
| `POST /api/auth/login` | público | identificador (documento **o** correo) + contraseña → token |
| `POST /api/auth/forgot-password` | público | emite token de recuperación y manda correo |
| `POST /api/auth/reset-password` | público | canjea el token y escribe la contraseña nueva |
| `PATCH /api/auth/change-password` | autenticado | cambio estando dentro; el sujeto sale del token |

### Qué clases

- `config/SecurityConfig.java` — la cadena de filtros. **Todo cerrado salvo lo que se abre aquí**
  (`anyRequest().authenticated()`), y las rutas públicas están listadas **una a una**, no con
  comodín: un endpoint nuevo bajo `/api/auth` nace protegido.
- `security/jwt/service/JwtService.java` — firma y valida. HMAC-SHA256, clave desde `JWT_SECRET`
  decodificada **una vez al arrancar** (`@PostConstruct`): un secreto mal puesto revienta el arranque
  con un mensaje claro en vez de fallar en el primer login. Exige ≥32 bytes.
- `security/jwt/filter/JwtAuthenticationFilter.java` — lee `Authorization: Bearer`, valida y puebla
  el `SecurityContext`. Un token caducado o basura es una petición *no autenticada*, no un 500.
- `security/userdetails/CustomUserDetails(+Service)` — el `username` es **el correo si lo hay, y si
  no el documento**; `isEnabled()` devuelve `usuario.activo`, así que desactivar a una persona
  bloquea su acceso de verdad (vía el trigger que sincroniza persona→usuario).
- `security/OwnershipGuard.java`, `security/AuthenticatedUser.java` — el «sobre quién», y el id de
  usuario para auditoría.
- `security/handler/SecurityJsonResponse.java` — 401/403 en JSON; sin esto Tomcat devuelve HTML.
- `auth/token/TokenSecrets.java` — **SHA-256, no BCrypt**, para los tokens de un solo uso. La razón
  está escrita en la clase: un token tiene 256 bits de aleatoriedad (no hay diccionario que lo
  contenga), y sobre todo **BCrypt lleva sal aleatoria, así que no se podría buscar por él**. SHA-256
  es determinista → columna indexable y canje en una consulta. Las *contraseñas* sí van con BCrypt.
- `auth/token/service/PasswordTokenService.java` — la mecánica: emitir, hashear, validar, **quemar**
  (un solo uso) y barrer los caducados de madrugada (`@Scheduled` con cron configurable).

### Cómo fluye un login

1. `AuthService.login` delega en `AuthenticationManager` → `CustomUserDetailsService.loadUserByUsername`
   usa `findByIdentifierWithRoles` (**JOIN FETCH**: los roles tienen que venir cargados, porque quien
   llama a `getAuthorities()` es el filtro, con la sesión de Hibernate ya cerrada).
2. Si falla, `BadCredentialsException` → 401 desde el `GlobalExceptionHandler` (no desde el entry
   point: esto ocurre ya dentro del controller).
3. Si pasa, se firma el token y se arma `AuthResponse` **desde el principal**, sin segunda consulta.

### Decisiones tomadas

- ✅ **Un mensaje único para credenciales incorrectas** ("Usuario o contraseña incorrectos"), para no
  confirmar qué documentos/correos existen.
- ⚠️ **Dos excepciones deliberadas a lo anterior, ya decididas, que una auditoría de seguridad va a
  marcar:** (1) cuenta desactivada responde con `code: CUENTA_DESACTIVADA` distinguible, y
  (2) `forgot-password` responde **404 si el correo no existe** — cambiado el 12-sep por decisión de
  producto (UX clara sobre seguridad estricta, público mayor y entorno cerrado). Está escrito en el
  javadoc de `PasswordRecoveryService` con la nota «si una auditoría marca esto, consúltese antes de
  arreglarlo». **Decisión a confirmar antes de la v1: se mantiene o se revierte.**
- 🔴 **No hay refresh token.** El JWT dura **1 hora** (`jwt.expiration-in-ms=3600000`) y no hay forma
  de renovarlo: a la hora, la sesión se cae y hay que volver a entrar. La tabla `refresh_token` existe
  en `V1__INITIAL_DDL.sql` (con su índice y su columna `user_agent`) y la propiedad
  `jwt.route.refresh.path` está en `application.properties`, pero **no hay una sola línea de Java que
  las use** (verificado: cero coincidencias de `refresh` en `src/main/java`). Para una demo de una
  hora no se nota; para operar un día entero en recepción, sí. **Decisión: subir la expiración para la
  v1, o implementar el refresh.**

---

## 2. Cuentas, roles y acceso

**Qué resuelve.** Toda persona registrada nace con cuenta; los roles deciden qué puede hacer.

### Por dónde entra

| Endpoint | Permiso |
|---|---|
| `GET /api/usuarios` (`?buscar=`), `GET /api/usuarios/{id}`, `/persona/{personId}` | ADMIN |
| `GET /api/usuarios/profesores` | CLASS_OPS |
| `PATCH /api/usuarios/{id}/activar` · `/desactivar` | ADMIN |
| `GET /api/roles`, `/{id}`, `/nombre/{name}` | ADMIN (solo lectura: los roles son semilla de `V1`) |
| `PATCH /api/personas/{id}/reiniciar-password` | DESK |

### Qué clases

`modules/user/service/UserService.java` · `modules/user/repository/UserProcedureRepository.java` ·
`modules/role/**` · `modules/user/controller/UserController.java`.

### Decisiones

- ✅ **No hay endpoint de alta de cuenta.** La cuenta nace con la persona (`POST /api/personas`), en
  la misma transacción. Una persona sin forma de entrar no debería poder existir.
- ✅ **La contraseña se hashea en Java y el procedimiento la recibe ya hecha**: la contraseña en claro
  nunca llega a la base.
- ✅ **Roles en una sola consulta por página** (`withRoles`): antes la pantalla de roles pedía los
  roles con una petición HTTP por usuario — veinte usuarios, veintiuna peticiones.
- ✅ **El selector de profesores incluye a los ADMIN**, y la validación de `sp_crear_sesion_manual`
  dice lo mismo. Si divergieran, el selector ofrecería opciones que fallan al guardar.
- ✅ **`sp_quitar_rol` se niega a dejar a un usuario sin ningún rol.**
- ✅ **Reiniciar contraseña = volver al número de documento**, sin token: quien lo ejecuta tiene
  delante a la persona y su documento. Es la única salida para quien se dio de alta sin correo (el
  caso normal del kiosko), que nunca podrá usar "olvidé mi contraseña". El documento **no viaja en la
  respuesta**, para no dejar una contraseña en claro en el historial del navegador.
- ⚠️ **Desactivar cuenta ≠ desactivar persona.** Son dos endpoints y hacen cosas distintas (uno
  bloquea el acceso dejando a la persona activa; el otro apaga a la persona y el trigger propaga a la
  cuenta). Es correcto pero se presta a confusión en la interfaz: conviene revisar que el front no los
  mezcle.

---

## 3. Personas, datos personales y habeas data

**Qué resuelve.** El registro de quién asiste, y el cumplimiento de la Ley 1581 / Decreto 1377
(autorización de tratamiento, datos sensibles, datos de terceros, revocación).

### Por dónde entra

| Endpoint | Permiso | Nota |
|---|---|---|
| `POST /api/personas` | DESK | alta de mostrador; exige declarar el **formato físico firmado** |
| `POST /api/personas/autoregistro` | **público** | kiosko; exige **dos casillas** en el body |
| `GET /api/personas` (`?buscar=`) | STAFF | búsqueda por nombre/apellido/documento, sin tildes |
| `GET /api/personas/buscar?documento=` | PERSON_LOOKUP | datos mínimos, **sin dinero** (terminal) |
| `GET /api/personas/{id}` · `/perfil` · `/saldo` · `/saldo-asistencias` | STAFF + self | |
| `PUT /api/personas/{id}` | DESK | reemplazo completo, **incluye documento** |
| `PUT /api/personas/{id}/mis-datos` | DESK + self | nombres, teléfono, fecha nac. **sin documento** |
| `PUT /api/personas/{id}/observaciones-salud` | DESK + self | dato sensible, consentimiento expreso |
| `PATCH /{id}/desactivar` · `/reactivar` | DESK | reactivar queda auditado |
| `DELETE /api/personas/{id}` | ADMIN | eliminación definitiva con anonimización |
| `POST /{id}/solicitar-cambio-correo` | DESK + self | no cambia nada: manda enlace al correo **nuevo** |
| `POST /api/personas/confirmar-cambio-correo` | **público** | canjea el token |
| `GET`/`POST /api/personas/{id}/consentimientos` | STAFF+self / DESK+self | estado e historial; revocar |
| `GET`/`POST /api/personas/{id}/contactos-emergencia` | STAFF + self | hasta 5 |
| `PUT`/`DELETE /api/contactos-emergencia/{contactId}` | STAFF + OR_OWNS_CONTACT | |

### Qué clases

`auth/service/RegisterService.java` (orquesta el alta) · `modules/person/service/PersonService.java` ·
`modules/person/service/EmergencyContactService.java` · `modules/consent/service/ConsentService.java` ·
`auth/service/EmailChangeService.java` · `modules/person/repository/PersonProcedureRepository.java`.

### Cómo fluye un alta (las dos vías)

`RegisterService.register(...)`, **todo en una transacción**:

1. `validateIsAdult(...)` — 18 años, regla de la v1.
2. `validateConsent(...)` — **la vía decide qué se exige**: mostrador → `signedPhysicalConsent`;
   autoregistro → `dataProcessingConsent` **y** `voluntaryParticipationConsent`, con mensajes
   distintos porque son dos casillas distintas en pantalla.
3. `personService.createPerson(...)` → `sp_registrar_persona`. La **fuente de registro la fija la
   ruta, nunca el body**: si viniera del cliente, cualquiera podría marcar un alta manual como
   autoregistro.
4. `recordRegistrationConsent(...)` — la constancia probatoria (art. 9). `registrado_por` = quién lo
   afirmó, o **null en el autoregistro** (no hay sesión: null significa "lo marcó ella misma").
5. `userService.createForPerson(...)` → `sp_registrar_usuario_para_persona` (rol MIEMBRO, contraseña
   = documento si no se envió otra).
6. `invitationService.invite(...)` — emite token de 48 h y publica el evento del correo, que sale
   **después del commit**.

### Decisiones

- ✅ **Tres endpoints de edición y no uno, por permisos.** El `PUT` completo reescribe el documento
  — abrirlo al propio miembro sería una vía de suplantación. `/mis-datos` no tiene el documento en el
  DTO: no hace falta acordarse de ignorarlo. `/observaciones-salud` toca una sola columna y por eso
  puede abrirse al dueño.
- ✅ **Datos de salud, regla asimétrica y documentada** (`PersonService.updateHealthObservations`):
  escribir exige consentimiento expreso (400 si falta, aunque el front ya lo pida — ese candado se
  salta llamando a la API); **borrar no exige nada**, y además revoca el consentimiento. Revocar el
  consentimiento de salud **borra el dato en la misma transacción** (`ConsentService.record`): una
  revocación con el dato todavía guardado no significaría nada.
- ✅ **El consentimiento viaja dentro de la operación que lo motiva**, no en una llamada aparte:
  `ConsentService` usa propagación `REQUIRED` a propósito. Dos llamadas separadas se pueden partir por
  la mitad y dejar persona sin constancia, o al revés.
- ✅ **El consentimiento solo se inserta, nunca se actualiza.** Revocar es una fila nueva con
  `granted=false`. "Una prueba que se puede editar no prueba nada."
- ✅ **La versión de la política se congela en cada fila** desde `app.privacy.policy-version` (hoy
  `1.0`), sin tabla de versiones.
- ✅ **Eliminación = anonimización, no borrado del rastro.** `sp_eliminar_persona` reasigna llegadas,
  movimientos, asistencias e historial al **sentinel 999999999** ("Usuario Eliminado") y borra
  persona, cuenta, contactos y **consentimientos**. Exige `force=true`.
- ✅ **Cambio de correo en dos pasos.** `POST /solicitar-cambio-correo` no escribe en `usuario`: manda
  un enlace al buzón **nuevo** (72 h). Un PUT directo sobre el correo sería secuestro de cuenta.
- ✅ **Teléfono duplicado → 409 informativo, no restricción de base.** No hay UNIQUE en
  `persona.telefono`: es un aviso para el mostrador, porque casi siempre significa "esta persona ya
  estaba registrada" y a veces significa "viven en la misma casa".
- ✅ **Hasta 5 contactos de emergencia**, todos del mismo peso, garantizado por **trigger** (un CHECK
  no puede contar filas de su propia tabla).
- ⚠️ **El autoregistro es público y no tiene freno**: ni rate limit, ni captcha. Crea persona, cuenta
  y **dispara un correo** (quema créditos de Brevo). En una red local no es problema; expuesto a
  internet, sí. **Decisión para la v1:** dejarlo (kiosko en LAN), o poner un límite por IP.

---

## 4. Check-in y cola del día

El corazón del sistema y el apartado con más historia detrás.

**Qué resuelve.** Registrar que alguien llegó, meterlo en la fila de la clase correcta, y decirle al
mostrador **cuánto cobrarle**.

### Por dónde entra

| Endpoint | Permiso | Método guardado |
|---|---|---|
| `POST /api/checkin/qr` | CLASS_OPS | `QR` |
| `POST /api/checkin/documento` | TERMINAL_QUEUE | `DOCUMENTO` (terminal del lobby) |
| `POST /api/checkin/manual` · `/manual/documento` | TERMINAL_QUEUE / DESK | `MANUAL` |
| `POST /api/checkin/propio` | MEMBER | `PROPIO` — **sin body**, la persona sale del token |
| `GET /api/checkin/propio` | MEMBER | su llegada de hoy, o 204 |
| `POST /api/cola` | TERMINAL_QUEUE | alta de la terminal |
| `GET /api/cola/admin` | STAFF | la fila **con dinero** |
| `GET /api/cola/resumen` | STAFF | sesión vigente + dos cifras de recaudo |
| `GET /api/cola/espera` · `/prepagada` · `/pendientes` | TERMINAL_QUEUE | pantallas de sala, **sin dinero** |
| `GET`/`PUT /api/cola/configuracion` | STAFF / ADMIN | personas por turno, margen, visibilidad |

### Qué clases

`checkin/service/impl/CheckinService.java` · `checkin/repository/CheckinProcedureRepository.java` ·
`queue/service/impl/QueueService.java` · `queue/view/{TodayQueueView,WaitingQueueView,PrepaidQueueView}.java` ·
`queue/event/{QueueChangedEvent,QueueEventBroadcaster}.java`.
En SQL: `sp_registrar_llegada`, `fn_obtener_o_crear_sesion_del_momento`, `fn_monto_a_pagar`,
`fn_estado_cuenta`, `fn_estado_sesion`, `vista_fila_hoy`.

### Cómo fluye una llegada

1. **A qué sesión entra** lo decide `fn_obtener_o_crear_sesion_del_momento`, por este orden:
   (1) la sesión abierta ahora → (2) la próxima de hoy **si empieza dentro de la próxima hora**
   → (3) el horario vigente, creando la sesión → (4) `75002` "fuera de horario".
   El paso 2 es regla de producto del 20-sep: si la clase de las 18:00 acabó y hay otra a las 20:00,
   quien llega a las 19:10 viene a esa — antes se le sumaba a la clase terminada y se le cobraba una
   sesión que no iba a recibir.
2. **La válvula de fuera de horario la da el método, no un parámetro**: solo `MANUAL` puede saltarse
   la regla, porque detrás hay una persona del mostrador viendo el caso. Los canales automáticos
   (propio, QR, documento) no pueden pedirla — no hay forma de solicitarla desde el cliente.
3. `sp_registrar_llegada` es **idempotente**: si ya hay llegada de esa persona en esa sesión,
   devuelve la existente en vez de crear otra.
4. Patrón H: se relee `vista_fila_hoy` para armar la respuesta (nombre, posición, montos).
5. **Confirmación automática por crédito** (`confirmIfCoveredByCredit`): si `cubierto_por_saldo` es
   cierto —no debe nada *y* aún no tiene asistencia— se confirma la asistencia en el acto a nombre
   del **usuario SISTEMA (999999998)** y se descuenta la sesión del crédito. La idempotencia es **por
   construcción**: la vista pone esa columna en `false` en cuanto existe la asistencia.
6. Se publica `QueueChangedEvent.checkin(sessionId)`, que sale a las pantallas **después del commit**.

### Las tres vistas de cola, y por qué son tres

| Vista | Endpoint | Lleva dinero |
|---|---|---|
| `vista_fila_hoy` → `TodayQueueView` | `/api/cola/admin` | **sí**: saldo, reembolsable, vuelto, monto a pagar |
| `vista_fila_espera` → `WaitingQueueView` | `/api/cola/espera` | no |
| `vista_fila_prepagada` → `PrepaidQueueView` | `/api/cola/prepagada` | no |

Siguen separadas **hasta el final del recorrido** (tres entidades, tres DTO, tres métodos del mapper)
para que la pantalla de la TV no pueda cargar montos ni por accidente.

### La cifra del cobro — lo más delicado del sistema

`fn_monto_a_pagar(persona, llegada)`:

- si la asistencia **ya** está confirmada → `GREATEST(-saldo, 0)` (solo la deuda vieja);
- si **no** → `GREATEST(tarifa - saldo, 0)` (la clase de hoy más la deuda, menos lo que tenga a favor).

El dinero **no se imputa a mano** contra la deuda y luego contra la clase: todo entra al mismo saldo,
de modo que "primero lo viejo, después lo de hoy" se cumple por construcción. Esta función existe
porque el frontend lo estaba deduciendo y salían tres cosas mal a la vez (no se saldaba la deuda, se
cobraba dos veces a quien tenía crédito, y el vuelto pendiente se leía como deuda).

### Decisiones

- ✅ **`PROPIO` es un valor propio del enum**, no `DOCUMENTO` reutilizado. Antes recepción veía
  "Terminal PSR" para alguien que había marcado desde el teléfono: **el dato guardado era el que
  mentía**, no la pantalla.
- ✅ **`POST /checkin/propio` no lleva body.** Abrir `/api/cola` a MIEMBRO habría dejado marcarle la
  llegada a otro cambiando el `personId`. Responde **201** la primera vez y **200** si ya existía.
- ✅ **`/api/cola/admin` devuelve por defecto solo la sesión vigente**, no el día entero: antes se
  mezclaban los de la clase de la mañana con los que esperan ahora. `?sesionId=` y `?todas=true` para
  lo demás.
- ✅ **`/api/cola/resumen` resuelve la sesión vigente en el servidor**, desde la tabla y no desde las
  filas de la cola: una clase en curso sin nadie en fila seguía siendo la vigente, y era el caso en
  que "Finalizar sesión" apuntaba a la clase equivocada.
- ✅ **Al profesor se le mandan las cifras en `null`, no se omite el campo**: así el cliente distingue
  "no puedo verlo" de "es cero".
- ⚠️ **`accountStatus`, `sessionStatus` y `paymentStatus` son `String`, no enums**, de punta a punta
  (los calcula una función SQL, no son columnas enumeradas). Es coherente y no tiene el fallo de
  `valueOf`, pero obliga al cliente a comparar cadenas. **Decisión: se deja así o se tipa en la v2.**

---

## 5. Asistencias

**Qué resuelve.** Confirmar que quien llegó entró a clase, y cobrarle la tarifa en ese momento.

### Por dónde entra

| Endpoint | Permiso |
|---|---|
| `PUT /api/llegadas/{id}/asistencia` | STAFF — confirma, sin body |
| `DELETE /api/llegadas/{id}/asistencia` | STAFF — anula |
| `GET /api/llegadas/{id}/asistencia` | STAFF |
| `GET /api/asistencias/historial` (`/persona/{id}`, `/sesion/{id}`, `?desde&hasta`) | STAFF (+self) |
| `GET /api/personas/{id}/saldo-asistencias` | STAFF + self |

### Qué clases

`attendance/service/impl/AttendanceService.java` · `attendance/repository/AttendanceProcedureRepository.java` ·
`attendance/view/{AttendanceHistoryView,AttendanceBalanceView}.java`. En SQL: `sp_registrar_asistencia`,
`sp_anular_asistencia`, `vista_historial_asistencias`, `vista_saldo_por_asistencia`.

### Cómo fluye

`sp_registrar_asistencia` inserta con `monto_cobrado = fn_tarifa_vigente()` — **el precio se congela
en el momento de confirmar**, no se recalcula después. El UNIQUE sobre `llegada_id` impide confirmar
dos veces ("la llegada ya fue atendida"). Anular es un `DELETE`, así que **devuelve la sesión al
crédito** automáticamente: el saldo es `abonado - cobrado`, y al desaparecer la fila deja de restar.

### Decisiones

- ✅ **El servicio no valida nada** (llegada, usuario, monto): el procedimiento ya lo hace. "Duplicar
  esas comprobaciones en Java solo agrega consultas y dos fuentes de verdad."
- ✅ **Sin `set_config`**: el trigger de auditoría de asistencia es `AFTER UPDATE`, y aquí solo se
  inserta y se borra.
- ⚠️ **El evento que publica va con `sessionId = null`** para no resolver dos relaciones LAZY. El
  cliente relee igual, pero si algún día una pantalla quiere filtrar por sesión, no puede.

---

## 6. Dinero: tarifa, métodos de pago, abonos, vueltos y reembolsos

**Qué resuelve.** Todo lo que entra y sale de la caja, con su rastro.

### El modelo, en cuatro cifras que **no** son la misma

| Cifra | Qué responde | Dónde vive |
|---|---|---|
| **saldo** | ¿cuánto crédito neto le queda? (`abonado - cobrado`) | `vista_saldo_persona` |
| **reembolsable** | ¿cuánto se le puede **devolver**? | `fn_saldo_reembolsable` |
| **vuelto pendiente** | ¿cuánto dinero **suyo** sigue en la caja? | `fn_vuelto_pendiente` |
| **monto a pagar** | ¿cuánto se le cobra **hoy**? | `fn_monto_a_pagar` |

La distinción saldo/reembolsable costó un fallo real: quien abonó 15.000 y consumió 10.000 en clases
tiene 5.000 a favor; devolverle los 15.000 dejaba la cuenta **debiendo 10.000 de clases ya dictadas**.
`fn_saldo_reembolsable` toma el **menor** de dos límites (lo no devuelto de cada abono, y el saldo) y
es **la única definición de la fórmula**: la usan a la vez la vista que muestra el número y la
validación que lo acepta, para que no puedan divergir.

### Por dónde entra

| Endpoint | Permiso |
|---|---|
| `GET /api/tarifa` | STAFF + MEMBER + terminal — el precio no es información interna |
| `PUT /api/tarifa` | ADMIN — auditado por trigger |
| `GET /api/metodos-pago` (`?activo=`), `/{id}` | DESK |
| `POST`/`PUT /api/metodos-pago/{id}`, `PATCH /{id}/activar` · `/desactivar` | ADMIN |
| `POST /api/movimientos` | DESK — abono |
| `POST /api/movimientos/{id}/reembolso` | DESK — contra un abono concreto |
| `POST /api/personas/{personId}/reembolso` | DESK — por persona, reparte FIFO |
| `PATCH /api/movimientos/{id}` | DESK — corrección, **auditada** |
| `POST /api/movimientos/{id}/vuelto` | DESK — entregar el vuelto |
| `POST /api/movimientos/{id}/abonar-vuelto` | DESK — convertirlo en crédito |
| `GET /api/movimientos/pendientes/vueltos[/persona/{id}]` | DESK (+self) |

### Qué clases

`payment/service/impl/AccountMovementService.java` (356 líneas, el núcleo) ·
`payment/repository/AccountMovementProcedureRepository.java` · `payment/repository/AccountMovementRepository.java` ·
`payment/controller/{AccountMovementController,PersonRefundController,PaymentMethodController}.java` ·
`pricing/**` · `shared/persistence/AuditContextRepository.java`.

### Las tres salidas de un vuelto

Alguien paga una clase de 4.000 con un billete de 50.000 y no hay cambio. Quedan 46.000 pendientes, y
hay **tres** caminos, todos implementados:

1. **Entregarlo** — `POST /{id}/vuelto`: marca `vuelto_entregado = true` sobre el abono original.
   **No crea un movimiento nuevo**, así que el método de pago es el del cobro.
2. **Abonarlo** — `POST /{id}/abonar-vuelto`: sube `monto_abonado` del movimiento original. **No entra
   dinero nuevo**: crear un abono aparte contaría los mismos billetes dos veces e inflaría el recaudo.
3. **Dejarlo pendiente** — aparece en `vista_vueltos_pendientes` y en el estado `VUELTO_PENDIENTE`.

### Auditoría del dinero — `app.usuario_id`

Los triggers de auditoría llaman a `fn_usuario_actual()`, que **falla si nadie publicó el usuario en
la transacción**. Es a propósito: sin eso, la auditoría sería opcional. `AuditContextRepository.setCurrentUser`
se llama en los cinco caminos que hacen `UPDATE` sobre tabla auditada: corregir movimiento, abonar
vuelto, entregar vuelto, tarifa y configuración de cola. Los procedimientos que auditan por parámetro
(cancelar sesión, reactivar/eliminar persona) no lo necesitan.

### Decisiones

- ✅ **Reembolso por persona = una transacción, reparto FIFO.** Antes el frontend encadenaba N
  llamadas y un fallo a mitad dejaba dinero devuelto a medias sin forma de saber cuánto. El tope se
  comprueba **antes** de emitir nada, para que el mensaje hable del total y no de un abono suelto.
- ✅ **El tope de un abono descuenta lo ya devuelto** (`sp_registrar_reembolso`). Sin eso, un mismo
  abono de 2.500 se podía reembolsar cuantas veces se quisiera: "un agujero de dinero, no una
  imprecisión".
- ✅ **El reembolso hereda la `llegada_id` del abono**, para que la nota de la fila deje de anunciar
  un cobro que ya se devolvió. Efecto secundario conocido y aceptado: **resta del recaudo de esa
  sesión**.
- ✅ **Un método de pago desactivado se rechaza en el servidor**, no solo se oculta del selector.
- ✅ **Los métodos de pago nunca se borran** (hay movimientos que los referencian): se desactivan, con
  `PATCH` y no con el `PUT`, que exigiría reenviar el nombre y pisaría un renombrado concurrente.
- ✅ **`collectedToday` cuenta lo ABONADO, no lo recibido**, neto de reembolsos: un billete grande no
  puede multiplicar la cifra del día por diez.
- ⚠️ **No se puede cobrar por transferencia y devolver el vuelto en efectivo** (una sola fila, un solo
  método). Registrarlo exigiría una columna nueva. Está en `PENDIENTES-V2.md` §3: **mientras la base
  se pueda recrear de cero es barato; después, caro.** Decisión a tomar *antes* de tener datos reales.

---

## 7. Sesiones, horarios y automatismos

**Qué resuelve.** Que exista una clase a la que llegar, con su profesor y su hora, sin que nadie
tenga que crearla a mano cada día.

### Por dónde entra

| Endpoint | Permiso |
|---|---|
| `POST /api/sesiones` · `DELETE /{id}` (cancelar) | CLASS_OPS |
| `PUT /api/sesiones/{id}/cerrar` | CLASS_OPS **+ solo las suyas si es profesor** |
| `PUT /api/sesiones/{id}/profesor/{teacherUserId}` | CLASS_OPS |
| `GET /api/sesiones` (`?desde&hasta&estado=activas\|canceladas\|todas`) | STAFF |
| `GET /api/sesiones/mias` | CLASS_OPS — el profesor sale del token |
| `GET /api/sesiones/{id}/asistentes` · `/resumen` · `/resumenes[...]` | STAFF |
| `GET/POST/PUT /api/horarios[...]`, `PATCH /{id}/activar` · `/desactivar` | STAFF / ADMIN |
| `POST /api/horarios/generar-sesiones` | ADMIN — válvula de soporte |
| `POST /api/horarios/{id}/excepciones`, `GET/DELETE /api/horarios/excepciones[...]` | STAFF / ADMIN |

### Qué clases

`session/service/impl/SessionService.java` · `schedule/service/impl/ScheduleService.java` ·
`scheduler/SessionCreationScheduler.java` · `scheduler/SessionClosingScheduler.java` ·
`scheduler/SessionStatusScheduler.java` · `scheduler/ScheduleChangeListener.java`.

### Los tres schedulers

| Clase | Cada | Qué hace |
|---|---|---|
| `SessionCreationScheduler` | 1 h (arranca al minuto) | crea las sesiones de los próximos **7 días** desde los horarios activos |
| `SessionClosingScheduler` | 15 min | cierra las sesiones sin `hora_fin` pasadas **2 h** de gracia, a nombre de SISTEMA |
| `SessionStatusScheduler` | 1 min | **avisa** (no escribe) cuando una sesión empieza o acaba por el reloj |

Y un listener: `ScheduleChangeListener` genera las sesiones de un horario **en cuanto ese horario
cambia** (`AFTER_COMMIT` + `REQUIRES_NEW`), sin esperar a la pasada horaria. Si la generación falla,
el horario sigue guardado.

### Detalles que conviene conocer antes de tocar esto

- **`fixedDelay` y no `fixedRate`** en los tres: si una pasada se demora, la siguiente espera en vez
  de solaparse.
- **Los tres se tragan sus excepciones** a propósito: si falla una pasada, el scheduler tiene que
  seguir vivo para la siguiente.
- **`SessionStatusScheduler` abre la transacción a mano con `TransactionTemplate`** y su javadoc
  explica por qué: con `@Transactional`, el `catch` queda *dentro* de la transacción y el commit
  revienta fuera del catch con un `UnexpectedRollbackException` que no dice nada del fallo original.
  **Es el patrón correcto y el que hay que copiar.**
- **Cambiar la hora de un horario reprograma las sesiones futuras vacías**, pero **nunca las que ya
  tienen gente apuntada**: mover una clase a la que alguien se apuntó le cambia la cita sin decírselo.
- **Cerrar una sesión que ya terminó no hace nada** (guarda en `SessionService.close`): movía su hora
  de fin hacia adelante y la volvía "vigente" otra vez para el check-in.
- **`canClose` / `canCancel` viajan en la respuesta**, calculados en el controller según quién
  pregunta: "un botón que se pinta y luego responde 403 es peor que no pintarlo".
- **Una excepción de horario debe caer en el día de la semana de su horario** (trigger): antes se
  podía cancelar "el lunes" con una fecha que caía jueves, y el lunes real se seguía generando.

### Decisiones

- ✅ **Las sesiones se generan por adelantado** (decisión del 11-sep) y además siguen naciendo solas
  en el check-in si hiciera falta: los dos caminos conviven sin duplicar, porque el segundo reutiliza
  la que encuentre.
- ✅ **Horarios y métodos de pago no se borran, se desactivan.** `DELETE /api/horarios/{id}` se
  mantiene «solo por compatibilidad con el frontend que ya lo llama» y hace lo mismo que `PATCH
  /desactivar`. ⚠️ **Candidato a retirar en la v1** si el front ya migró.

---

## 8. Tiempo real (WebSocket / STOMP)

**Qué resuelve.** Que el panel de recepción y las dos pantallas de sala se actualicen solas, sin
polling.

### Qué clases

`config/WebSocketConfig.java` · `security/websocket/StompAuthChannelInterceptor.java` ·
`queue/event/QueueChangedEvent.java` · `queue/event/QueueEventBroadcaster.java`.

### Cómo funciona

- Broker **en memoria** (`enableSimpleBroker("/topic")`), tres topics: `/topic/cola/admin`,
  `/topic/cola/espera`, `/topic/cola/prepagada`. Un broker externo solo haría falta con varias
  instancias.
- **El mensaje no lleva datos**: es `{sessionId, type}` y nada más. Dos razones escritas en
  `QueueChangedEvent`: la pantalla de sala no debe recibir montos (RNF-03), y un payload congelado
  podría llegar ya desactualizado. **El cliente recibe la señal y relee por REST** el endpoint que ya
  tiene permitido.
- `QueueEventBroadcaster` escucha en **`AFTER_COMMIT`**: avisar dentro de la transacción dejaría a
  las pantallas mostrando una llegada que un rollback posterior borra. La difusión es *best-effort*:
  si el broker falla, se registra y ya — el dato está guardado.
- `topicsFor(...)` reparte: un abono no mueve la fila de espera; confirmar asistencia no cambia
  quién figura como prepagado.
- **La autenticación es en el frame CONNECT**, no en el handshake HTTP: SockJS no manda
  `Authorization` en sus transportes de respaldo, así que `/ws/**` va en `permitAll()` y el token
  viaja como cabecera nativa de STOMP.

### ⚠️ Qué decidir

**El interceptor autentica pero no autoriza por destino.** Cualquier usuario autenticado —incluido un
MIEMBRO— puede suscribirse a `/topic/cola/admin`, que por REST le está prohibido. Hoy no se filtra
nada porque el mensaje va vacío, pero **la defensa es que el payload esté vacío, no que el topic esté
cerrado**: el día que alguien meta datos en el mensaje (la optimización obvia para ahorrarse la
relectura), el agujero se abre solo.

---

## 9. Reportes y auditoría

**Qué resuelve.** Cuánto entró, cuánto generó cada clase, y quién cambió qué.

### Caja vs devengo — la distinción que hay que tener clara

| Vista | Pregunta | Agrupa por |
|---|---|---|
| `vista_recaudo_diario` | ¿cuánto dinero **entró** hoy? | fecha del **pago** |
| `vista_ingreso_diario` | ¿cuánto **generó** la clase de hoy? | fecha de la **sesión** |

Con la primera, un abono adelantado el lunes para la clase del miércoles cuenta como ingreso del
lunes y el miércoles aparece artificialmente flojo. El **dashboard usa la segunda**; el corte de caja
usa la primera. Además, en la segunda `total_asistencias` y `total_ingreso` salen de las **mismas
filas** y siempre cuadran entre sí.

### Por dónde entra

| Endpoint | Permiso |
|---|---|
| `GET /api/reportes/dashboard` (`?fecha&desde&hasta`) | DESK |
| `GET /api/reportes/recaudo-hoy` | DESK — la cifra del encabezado del check-in |
| `GET /api/reportes/recaudo-diario[/{fecha}]` · `/recaudo-por-sesion[/{id}]` | DESK |
| `GET /api/reportes/saldos-personas` | DESK — paginado, de deudor a acreedor |
| `GET /api/auditoria/historial-cambios[...]` | **ADMIN** |

### Auditoría: qué se registra de verdad

El historial lo alimentan **triggers y procedimientos, nunca un endpoint**. `tabla_auditable_enum`
declara seis tablas, pero en la práctica:

| Tabla | ¿Deja rastro? |
|---|---|
| `TARIFA` | sí, al cambiar el monto |
| `CONFIGURACION_COLA` | sí, campo por campo |
| `MOVIMIENTO_CUENTA` | sí: corregir, entregar vuelto, abonar vuelto |
| `SESION` | sí: cancelar y cierre automático |
| `PERSONA` | **parcial**: solo reactivar y eliminar. Editar nombre o teléfono **no deja rastro** |
| `ASISTENCIA` | **nunca**: el trigger es `AFTER UPDATE OF monto_cobrado` y ningún endpoint modifica ese campo |

Esto explica el "la auditoría no funciona" del testeo: **funciona, pero hay muy poco que auditar**.
Los reembolsos tampoco aparecen, y es correcto: son un `INSERT` de una fila nueva en negativo, no un
`UPDATE`; el rastro del reembolso es el movimiento mismo.

⚠️ **Decisión ya tomada y confirmada:** se deja así para la v1 — ampliarla toca esquema.
Y `campo` es el **nombre de la columna** (`activo`, `cancelada`, `personas_por_turno`): exacto e
ilegible. **La traducción a lenguaje humano la hace frontend**, ya decidido.

---

## 10. Correo

**Qué resuelve.** Cuatro correos transaccionales, sin que un SMTP caído pueda tumbar un alta.

### Qué clases

`email/service/EmailService.java` (decide **qué** correo) · `email/EmailSender.java` (interfaz de
transporte) · `email/SmtpEmailSender.java` (desarrollo) · `email/BrevoApiEmailSender.java`
(producción) · `auth/event/AuthEmailEvents.java` + `AuthEmailListener.java`.

### Cómo fluye

1. Un servicio de auth publica un evento (`Welcome`, `PasswordRecovery`, `PasswordChanged`,
   `EmailChangeRequested`) **con los datos ya extraídos dentro de la transacción** — `getPerson()` es
   perezoso y el listener corre con la sesión cerrada.
2. `AuthEmailListener` escucha en **`AFTER_COMMIT`**: el token tiene que estar confirmado antes de
   que salga el enlace, o la persona recibe un enlace muerto.
3. `EmailService` arma el HTML con Thymeleaf (plantillas en `resources/templates/email/`) y se lo pasa
   al `EmailSender` inyectado.
4. **El listener captura cualquier fallo y solo lo registra**: la cuenta ya existe y se puede entrar
   con el documento.

### Decisiones

- ✅ **Proveedor por configuración** (`app.email.provider=smtp|brevo`), con `@ConditionalOnProperty`:
  solo se registra un sender. Añadir un tercero no toca `EmailService` ni las plantillas.
- ✅ **Brevo por API HTTP y no por SMTP**: muchos hostings bloquean el 587, y la API devuelve el error
  real en el cuerpo.
- ✅ **Sin API key, el arranque revienta** (`@PostConstruct`): mejor eso que descubrirlo cuando el
  primer usuario se queda sin correo.
- ✅ **El código y el cuerpo de la respuesta de Brevo van en el mensaje de la excepción**: una key
  revocada (401), la cuota agotada (402), un remitente sin verificar (400) y una IP no autorizada
  (403) producían antes exactamente el mismo log.
- ✅ **El token nunca se escribe en los logs**; el destinatario sí.
- ✅ **Los enlaces apuntan al FRONT** (`app.frontend.url`), no al backend.
- ⚠️ **La causa raíz de Brevo sigue abierta** (`PENDIENTES-V2.md` §4): el backend ya dice *qué*
  responde Brevo; el *por qué* hay que mirarlo en el panel (créditos, key, remitente verificado).

---

## 11. Arranque, configuración y seeds — 🔴 el apartado a limpiar

**Qué resuelve.** Que una base recién migrada tenga con qué entrar y con qué probar. Es el problema
del huevo y la gallina: toda cuenta nace con rol MIEMBRO y ascender a alguien exige ser ADMIN.

### Qué hay hoy — tres capas de datos, y conviene no confundirlas

| Capa | Dónde | Se ejecuta | Qué crea |
|---|---|---|---|
| **1. Semilla del esquema** | `V1__INITIAL_DDL.sql` (final) | siempre, con Flyway | 5 roles, tarifa 4.000, config de cola, métodos EFECTIVO y TRANSFERENCIA, **persona/usuario sentinel 999999999**, **usuario SISTEMA 999999998** |
| **2. Cuentas de "producción"** | `config/ProductionAccountsSeeder.java` | **siempre, sin bandera** | 1 admin + 5 terminales, **con contraseñas escritas en el código** |
| **3. Datos de prueba** | `config/TestDataSeeder.java` (`@Order(1)`) | `app.seed.enabled` — **`true` por defecto** | 21 personas+cuentas (docs 1111–9999), cuenta `terminal@psraerobicos.com`, 5 horarios L–V |
| **3b. Histórico de prueba** | `config/HistoricalDataSeeder.java` (`@Order(2)`) | `app.seed.historical.enabled` — **`false`** | 45 personas (docs 70001+), 5 días hábiles, ~150 asistencias, dinero, vueltos, reembolsos, una sesión cancelada |

**La capa 1 es legítima y tiene que quedarse**: el sentinel y el usuario SISTEMA no son datos de
prueba, son **infraestructura**. `sp_eliminar_persona` reasigna al sentinel para conservar el hecho
sin la identidad, y el cierre automático de sesiones se audita a nombre de SISTEMA porque
`historial_cambio.usuario_id` es `NOT NULL`. Quitarlos rompe funcionalidad.

### Lo que está mal, con nombre y apellido

**a) Credenciales de producción literales en el fuente.** `ProductionAccountsSeeder` crea
`adminpsraerobicos@gmail.com` con una contraseña escrita en el archivo, más cinco terminales con
patrón predecible. **Corre sin bandera condicional**, y como es idempotente por correo, si alguien
borra la cuenta se vuelve a crear igual al reiniciar. Ese admin puede todo: dinero, auditoría, roles,
eliminar personas.

**b) El seed de prueba está encendido por defecto.** `app.seed.enabled=${SEED_ENABLED:true}`, con el
comentario *"NUNCA en producción: son credenciales conocidas"* justo encima. Desplegar sin acordarse
de poner `SEED_ENABLED=false` siembra 21 cuentas **cuya contraseña es su número de documento**
(1111, 2222, 3333, 4444…) y cinco horarios de 18:00 a 20:00 que probablemente no son los reales.
El histórico sí está apagado por defecto — que es como debería estar este.

**c) Los seeds ensucian, pero no de forma irreversible.** Los tres son idempotentes y **marcados**:
las personas de prueba viven en documentos 1111–9999, el histórico en 70001–70045, las terminales en
`@psraerobicos.com`. Eso hace que **se puedan borrar con un `WHERE` acotado**, no hay que recrear la
base por ellos.

### Las opciones reales, con su costo

| Opción | Qué implica | Costo | Riesgo |
|---|---|---|---|
| **A. Mínimo viable (recomendada para la v1)** | `SEED_ENABLED=false` en el entorno de producción; sacar las contraseñas del `ProductionAccountsSeeder` a variables de entorno y que **no cree nada si no están puestas**; cambiar la del admin ya desplegado | ~1 h | ninguno: no toca lógica |
| **B. A + invertir el defecto** | además, `${SEED_ENABLED:false}`: hay que **encenderlo** a mano en desarrollo, no apagarlo en producción | +15 min | que un desarrollador arranque sin datos y no sepa por qué (se cubre con una línea en el README) |
| **C. B + admin por arranque único** | el admin no lo crea un seeder: se crea con una migración o un comando de arranque que **exige** `ADMIN_BOOTSTRAP_PASSWORD` y obliga a cambiarla al primer login | +2–3 h | más código nuevo a 5 días del cierre |
| **D. Base limpia para producción** | recrear la base sin seeds y dar de alta todo a mano por la API | 2–4 h de trabajo manual + riesgo de teclear mal | **es el momento de hacerlo**: mientras la base se pueda recrear de cero, cuesta poco; con datos reales dentro, ya no |

**Lo que yo haría con 5 días:** **B para el código** (1 h, cierra el agujero) y **D para el entorno de
producción** cuando se monte, dejando el de demo con los seeds encendidos. Las dos cosas son
independientes y ninguna toca lógica de negocio — es el tipo de limpieza que se puede hacer a tres
días de una presentación sin miedo.

### El resto de la configuración

| Propiedad | Valor por defecto | Nota |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `none` | ✅ el esquema lo manda Flyway, no Hibernate |
| `spring.jpa.show-sql` / `logging.level.org.hibernate.SQL` | `true` / `DEBUG` | ⚠️ **apagar en producción**: llena el log y expone las consultas |
| `logging.level.org.flywaydb` | `DEBUG` | ⚠️ igual |
| `jwt.expiration-in-ms` | 3.600.000 (1 h) | ⚠️ ver §1, no hay refresh |
| `app.cors.allowed-origins` | los tres puertos de Vite | ⚠️ **no puede quedar en `*`** en producción |
| `app.password-token.*` | invitación 48 h, recuperación 30 min, limpieza 03:00 | ✅ |
| `app.email-change.expiration-hours` | 72 | ✅ ventana larga a propósito |
| `app.privacy.policy-version` | `1.0` | ✅ |
| `escapeSyntaxCallMode=callIfNoReturn` | | ✅ **no tocar**: sin esto todos los `CALL` fallan con "function does not exist" |

🔴 **No hay zona horaria fijada en ningún sitio.** Java usa `ZoneId.systemDefault()` y el SQL usa
`CURRENT_DATE`/`now()`: dos relojes, ninguno anclado. En local coinciden; en un servidor en UTC el
día cambia a las 7 de la tarde hora de Colombia. Se cierra con una variable de entorno — ver §14.

---

## 12. La capa de persistencia — inventario

Para saber dónde mirar cuando algo no cuadra. Todo en `src/main/resources/db/migration/`.

**Una sola migración versionada** (`V1__INITIAL_DDL.sql`) y cuatro repetibles (`R__FUNCTIONS`,
`R__TRIGGERS`, `R__STORED_PROCEDURES`, `R__VIEWS`), que Flyway reaplica cuando cambia su contenido.
La `V2__CHECKIN_PROPIO.sql` se **plegó dentro de V1 el 21-sep** y se borró: la base se iba a recrear
de cero y una migración incremental para un esquema que nunca existió era solo ruido.

### Funciones (`R__FUNCTIONS.sql`)

| Función | Responde |
|---|---|
| `fn_tarifa_vigente` | el precio de hoy (fila única) |
| `fn_dias_credito_disponible` | cuántas clases cubre el saldo |
| `fn_saldo_reembolsable` | cuánto se le puede devolver (el menor de dos límites) |
| `fn_vuelto_pendiente` | cuánto dinero suyo sigue en la caja |
| `fn_estado_cuenta` | `DEBE` / `VUELTO_PENDIENTE` / `AL_DIA` — **el orden de las ramas ES la regla** |
| `fn_monto_a_pagar` | **la cifra del cobro** |
| `fn_estado_sesion` | `UPCOMING` / `IN_PROGRESS` / `ENDED` / `CANCELLED`, con el reloj del servidor |
| `fn_validar_persona_activa` | lanza `75001` si está desactivada |
| `fn_sesion_abierta_ahora` · `fn_horario_vigente_ahora` · `fn_obtener_o_crear_sesion_del_momento` | a qué clase entra quien llega |
| `fn_usuario_actual` | el usuario de la transacción para auditoría (**falla si no se puso**) |

### Procedimientos (`R__STORED_PROCEDURES.sql`)

`sp_registrar_persona` · `sp_registrar_usuario_para_persona` · `sp_asignar_rol` · `sp_quitar_rol` ·
`sp_reactivar_persona` · `sp_registrar_llegada` · `sp_crear_sesion_manual` · `sp_cancelar_sesion` ·
`sp_registrar_asistencia` · `sp_anular_asistencia` · `sp_registrar_movimiento` ·
`sp_registrar_reembolso` · `sp_entregar_vuelto` · `sp_eliminar_persona` · `sp_cerrar_sesiones_vencidas`.

> **Trampa documentada:** en Postgres los parámetros `OUT` **forman parte de la identidad** de un
> procedimiento. Añadir uno no reemplaza el anterior: crea una sobrecarga y el `CALL` queda ambiguo.
> Por eso varios llevan `DROP PROCEDURE IF EXISTS` explícito delante. El síntoma no apunta al
> procedimiento — lo que se ve es que "toda alta de persona deja de funcionar".

### Vistas (`R__VIEWS.sql`)

`vista_saldo_persona` · `vista_fila_hoy` · `vista_fila_prepagada` · `vista_fila_espera` ·
`vista_vueltos_pendientes` · `vista_recaudo_diario` · `vista_ingreso_diario` ·
`vista_recaudo_por_sesion` · `vista_saldo_por_asistencia` · `vista_historial_asistencias` ·
`vista_historial_cambios` · `vista_perfil_persona` · `vista_resumen_sesiones`.

> **Dos reglas al tocarlas:** (1) `CREATE OR REPLACE VIEW` **solo sabe añadir columnas al final** — por
> eso las nuevas van siempre las últimas; quitar una exige `DROP VIEW` explícito. (2) Cada vista que
> alimenta una entidad JPA necesita un `@Id`: `vista_recaudo_diario` lleva un `ROW_NUMBER()`
> **sintético** solo para eso, y no sale a la API.

### Triggers (`R__TRIGGERS.sql`)

Auditoría de `movimiento_cuenta`, `asistencia`, `tarifa` y `configuracion_cola`; validación de
persona requerida por rol; sincronización persona→usuario; validación del día de una excepción;
límite de cinco contactos de emergencia.

> **Cuidado con `trg_validar_persona_requerida`:** Postgres castea los literales del `IN` al tipo del
> enum, así que un valor que no exista en `rol_nombre_enum` **tumba toda alta de usuario**. Primero se
> añade al enum, luego a la lista.

---

## 13. El contrato HTTP

| Situación | Status | `code` |
|---|---|---|
| Validación de DTO, parámetro mal tipado, JSON roto, falta `@RequestParam` | 400 | — |
| Regla de negocio de plpgsql (`P0001`) | **400** | — (el texto del `RAISE`) |
| CHECK del esquema (`chk_sesion_horas`, `chk_contacto_emergencia_no_vacio`) | 400 | — |
| Credenciales incorrectas o cuenta inexistente | 401 | — |
| Cuenta desactivada | 401 | `CUENTA_DESACTIVADA` |
| Rol insuficiente | 403 | — |
| Ruta inexistente | **404** | — |
| Recurso inexistente | 404 | — |
| Duplicado / conflicto de estado | 409 | — |
| Persona desactivada (`75001`) | 409 | `PERSONA_DESACTIVADA` |
| Sin clase vigente (`75002`) | 409 | `SIN_SESION_ACTIVA` |
| Fallo nuestro (`75000`) o cualquier otra cosa | 500 | — |

Todas las respuestas de error son `ErrorResponse {status, code, message, timestamp}`. El campo `code`
solo viaja en los tres casos que el frontend necesita distinguir por máquina; el resto se muestran
por su mensaje.

⚠️ **Bordes conocidos:** algunos choques de estado que nacen de un `RAISE` salen como **400 y no 409**
("la llegada ya fue atendida", "la sesión ya estaba cancelada"). El mensaje es correcto; el status no
del todo. Si el front decide por el status, hay que mirarlo.

---

## 14. Problemas conocidos — para tener presente mientras se audita

No son el foco de este documento, pero conviene conocerlos al decidir qué se toca. Los seis defectos
que se arreglaron el 11-sep (enums en vistas, rutas 404, `GET /api/tarifa` a MIEMBRO,
activar/desactivar método de pago, `chk_sesion_horas`, `?sort=` en los buscadores) **siguen
cerrados**, reverificados el 23-sep.

### Abiertos hoy

| # | Qué | Dónde | Efecto | Arreglo |
|---|---|---|---|---|
| 1 | 🔴 Credenciales en el código + seed encendido por defecto | `ProductionAccountsSeeder`, `application.properties` | acceso ADMIN para cualquiera que vea el repo | §11, opción B — **~1 h** |
| 2 | 🔴 No hay zona horaria fijada | todo el proyecto | en un servidor UTC, el día cambia a las 19:00 locales: sesiones con fecha equivocada, cola vacía, recaudo mal atribuido | `TZ=America/Bogota` + `-Duser.timezone` — **10 min** |
| 3 | ⚠️ El cierre automático de sesiones **avisa a nadie** | `SessionClosingScheduler:60` | publica el evento **sin transacción activa**, y `@TransactionalEventListener(AFTER_COMMIT)` lo descarta en silencio — las pantallas no se enteran | envolver en `TransactionTemplate` como hace `SessionStatusScheduler` — **15 min** |
| 4 | ⚠️ La relectura tras la confirmación automática no relee | `CheckinService.confirmIfCoveredByCredit` | la fila ya estaba en la caché de primer nivel, así que `/checkin/propio` responde `attended:false` aunque la asistencia sí se confirmó | `detach` antes de releer — **15 min** |
| 5 | ⚠️ El WebSocket no autoriza por destino | `StompAuthChannelInterceptor` | un MIEMBRO puede suscribirse a `/topic/cola/admin`; hoy no filtra datos porque el payload va vacío | §8 |
| 6 | ⚠️ `@DynamicUpdate` falta en `Checkin`, `Role`, `ChangeHistory` | sus entidades | hoy **nadie las escribe desde JPA** (verificado), pero el día que alguien añada un `save(...)` sobre una columna enum de Postgres, vuelve el 500 que ya apareció dos veces | añadir la anotación **solo si** se van a escribir |
| 7 | ⚠️ La fila de prepagados no filtra por sesión en su mitad principal | `QueueService.getPrepaidQueue` | con dos clases seguidas, alguien de la anterior puede seguir en "Pasan directo" durante la ventana de visibilidad | la vista no tiene `sesion_id`; requiere tocarla |
| 8 | ⚠️ No hay refresh token | §1 | sesión de 1 h sin renovación | subir la expiración, o implementarlo |

### Las dos causas raíz que explican casi todo el historial

1. **Traducción inglés/español** (§0.4). Cinco fallos reportados en tres rondas distintas eran el
   mismo error en sitios distintos.
2. **Lo que pasa dentro o fuera de la transacción no es lo que parece** (§0.3, §8). Los defectos 3 y 4
   de arriba son las dos caras de esto, y los dos **fallan en silencio**: ninguna excepción, ningún
   log, el código se lee bien.

---

## 15. Lo que este documento no puede decir

Honestidad sobre el alcance, porque afecta a cómo se lee todo lo anterior:

- **Nada de esto está ejecutado.** Es lectura de código. La base no se arranca desde el 9 de
  septiembre, y el SQL de las rondas 18, 19, 20 y 21 (cuatro funciones nuevas, dos columnas nuevas en
  `vista_fila_hoy`, el cambio del reembolso) **nunca se ha aplicado contra una base real**. Con
  `ddl-auto=none`, una columna mal escrita en una entidad no falla al arrancar sino al consultar:
  **ese tipo de fallo no lo encuentra una lectura**. La lista de qué comprobar en el primer arranque
  está en `PENDIENTES-V2.md` §2.
- **La cifra del cobro no está verificada con datos.** `fn_monto_a_pagar` y `cubierto_por_saldo`
  deciden cuánto se cobra y si la asistencia se confirma sola. Están razonadas sobre el modelo, no
  ejecutadas. La prueba de una línea: **persona con crédito 5 marca llegada → su perfil debe decir 4,
  no 3.**
- **Rendimiento sin medir.** Por lectura: `getPrepaidQueue` hace dos consultas y una lectura de
  config por llamada, y `currentSession()` trae todas las sesiones del día y filtra en memoria. Con
  dos o tres clases diarias es irrelevante.
- **No hay tests.** `src/test` está vacío. Cero cobertura automática: cada cambio se verifica
  ejecutando la aplicación. Es, con diferencia, **la mayor deuda técnica del proyecto**, y no es
  pagable en 5 días — pero conviene decirlo al hablar de "calidad del software" con el cliente.

---

## 16. Resumen para decidir

Lo que hay que resolver **antes** de la salida, por orden de daño si no se hace:

| Prioridad | Qué | Costo | Toca lógica |
|---|---|---|---|
| 1 | Sacar las credenciales del código e invertir `SEED_ENABLED` (§11 opción B) | ~1 h | no |
| 2 | Fijar la zona horaria del servidor y de la JVM | 10 min | no |
| 3 | **Formatear la base y arrancar**, con la lista de `PENDIENTES-V2.md` §2 | medio día | no |
| 4 | Probar el cobro con `DOCUMENTO`, `MANUAL` y `PROPIO`, y con una persona con crédito | 1 h | no |
| 5 | Los dos arreglos de transacción (§14 #3 y #4) | 30 min | sí, mínimo |
| 6 | Apagar `show-sql` y los `DEBUG`, fijar CORS | 10 min | no |
| 7 | Decidir la expiración del JWT (§1) | 5 min o 3 h | según la opción |

Lo que **no** hay que tocar antes de la v1: la auditoría de 4 tablas (§9, ya decidido), las tres
entidades sin `@DynamicUpdate` (§14 #6), y el modelo de reglas en la base (§0.2). Son decisiones
tomadas con su motivo escrito, y removerlas a cinco días del cierre solo añade riesgo.
