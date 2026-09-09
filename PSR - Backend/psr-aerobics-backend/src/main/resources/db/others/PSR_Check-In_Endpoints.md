# Endpoints — PSR Check-In

Especificación de la API REST: mecanismo de implementación, nombre de
DTO y forma del JSON de cada endpoint. Coincide con el esquema SQL
entregado.

## Leyenda

| Tipo | Significa |
|------|-----------|
| **CRUD** | Crear/leer/actualizar/(soft-)eliminar estándar, sin lógica relevante. JPA directo. |
| **VIEW** | Expone (casi 1:1) una vista SQL. Entidad JPA de solo lectura (`@Immutable`). |
| **PERSONALIZADA — JPA** | Lógica propia resoluble sin SQL nativo. |
| **PERSONALIZADA — PSQL** | Necesita un procedimiento (`CALL`) o un cálculo que no vale la pena reconstruir en JPA. |

⏳ marca los endpoints que todavía **no tienen controller**. El resto de la tabla refleja
las rutas tal como están implementadas hoy.

## Convención de DTOs

Nombres en inglés, igual que las entidades. Traducción tabla → entidad:

| Tabla | Entidad | Tabla | Entidad |
|---|---|---|---|
| `persona` | `Person` | `movimiento_cuenta` | `AccountMovement` |
| `usuario` | `User` | `metodo_pago` | `PaymentMethod` |
| `rol` | `Role` | `tarifa` | `Pricing` |
| `contacto_emergencia` | `EmergencyContact` | `configuracion_cola` | `QueueConfig` |
| `horario` | `Schedule` | `historial_cambio` | `ChangeHistory` |
| `horario_excepcion` | `ScheduleException` | `token_recuperacion` | `RecoveryToken` |
| `sesion` | `Session` | `llegada` | `Checkin` |
| `asistencia` | `Attendance` | | |

Sufijos: `…Request` (entrada), `…Response` (salida de tabla),
`…ViewResponse` (salida de vista SQL, solo lectura).

El sufijo largo evita el choque de nombres con las entidades `@Immutable` del paquete
`view/`, que se llaman `…View` y son las que mapean la vista.

### Cuándo reutilizar un DTO y cuándo crear uno dedicado

Regla práctica para decidir, antes de duplicar:

| Solapamiento de atributos | Decisión |
|---|---|
| **≥ 80 %** | Reutilizar el mismo DTO. Duplicar aquí solo genera dos clases que hay que mantener sincronizadas. |
| **50–79 %** | Reutilizar **solo si** los campos que sobran son opcionales y su ausencia no confunde a quien consume la API. Si el consumidor tiene que preguntarse "¿por qué este campo viene en `null` siempre?", es dedicado. |
| **< 50 %** | DTO dedicado. Sin excepción. |

Dos reglas que ganan sobre el porcentaje:

1. **Nunca se reutiliza un `Response` como `Request`.** Un `Response`
   suele traer campos calculados por la base de datos (`amountCharged`,
   `saldo`, `posicion`) o de identidad (`id`, `markedAt`). Si el mismo
   DTO se usa de entrada, el cliente aparenta poder fijar valores que el
   backend va a ignorar — es la fuente de confusión más común en este
   proyecto.
2. **Un `Request` nunca lleva campos que el backend deriva.** Ver
   "Origen de cada campo" abajo.

### Origen de cada campo (crítico)

Todo campo de un `…Request` sale de una de estas tres fuentes. El
documento marca explícitamente cuál en cada endpoint:

| Origen | Marca | Significa |
|---|---|---|
| **Body** | (sin marca) | Lo envía el cliente en el JSON. |
| **Path** | 🔗 | Va en la URL (`/api/personas/{id}`), no se repite en el body. |
| **JWT** | 🔐 | Lo saca el backend del usuario autenticado. **Nunca viaja en el body**, aunque el procedimiento SQL lo reciba como parámetro. |
| **Derivado** | ⚙️ | Lo calcula la base de datos o el service. No existe en el `Request`. |

⚠️ Regla de seguridad: todo parámetro `usuario_id` /
`p_ejecutado_por_usuario_id` / `marcado_por` de un procedimiento se llena
con 🔐, **jamás** con un campo del body. Si viniera del body, un cliente
podría registrar acciones a nombre de otro usuario.

## Códigos de error propios

El sistema define dos `SQLSTATE` personalizados que **no son errores
genéricos** — el `GlobalExceptionHandler` debe distinguirlos, porque
cada uno lleva a una acción distinta en el frontend:

| Código | Significa | Qué debería hacer el frontend |
|---|---|---|
| **`75001`** | La persona existe pero está **desactivada** (distinto de "no existe") | Ofrecer "Usuario desactivado, ¿deseas reactivarlo?" → `PATCH /api/personas/{id}/reactivar` |
| **`75002`** | **No hay ninguna clase vigente** en este momento | "No hay clase ahora" — no es un error del usuario |

Los endpoints que pueden lanzarlos están marcados con ⚠️ abajo.

---

## Módulo `users/` (roles)

| Método | Endpoint | Tipo | DTO entrada | DTO salida |
|--------|----------|------|---|---|
| GET | `/api/usuarios` | CRUD | — | `Page<UserResponse>` |
| GET | `/api/usuarios/{id}` | CRUD | — | `UserResponse` |
| GET | `/api/usuarios/persona/{personId}` | CRUD | — | `UserResponse` |
| PATCH | `/api/usuarios/{id}/activar` | CRUD | — | `UserResponse` |
| PATCH | `/api/usuarios/{id}/desactivar` | CRUD | — | `UserResponse` |
| GET | `/api/roles` | CRUD | — | `RoleResponse[]` |
| GET | `/api/roles/{id}` | CRUD | — | `RoleResponse` |
| GET | `/api/roles/nombre/{nombre}` | CRUD | — | `RoleResponse` |
| GET | `/api/usuarios/{userId}/roles` | CRUD | — | `RoleResponse[]` |
| POST | `/api/usuarios/{userId}/roles` | PERSONALIZADA — PSQL | `UserRoleRequest` | `RoleResponse[]` |
| DELETE | `/api/usuarios/{userId}/roles/{roleName}` | PERSONALIZADA — PSQL | — | `204` |

`POST` → `CALL sp_asignar_rol(usuarioId, rolNombre)`, idempotente.
`DELETE` → `CALL sp_quitar_rol`, falla si dejaría al usuario sin ningún rol.

```jsonc
// UserRoleRequest
{
  "roleName": "PROFESOR"        // ADMIN | MIEMBRO | PROFESOR | RECEPCIONISTA
}
// userId 🔗 viene del path

// RoleResponse[]
[
  { "id": 1, "name": "ADMIN" },
  { "id": 3, "name": "PROFESOR" }
]
```

**Spring Security:** los roles se leen de `usuario_rol` (varios por
usuario), no de una columna — el `UserDetailsService` debe cargar la
lista completa de authorities, no un solo rol.

⚠️ `MIEMBRO`, `PROFESOR` y `RECEPCIONISTA` exigen que el usuario tenga
`persona_id` (trigger `trg_validar_persona_requerida`). Solo `ADMIN`
puede ser una cuenta sin persona. Asignar uno de los tres a un usuario
sin persona devuelve error de la base de datos.

---

## Módulo `auth/`

| Método | Endpoint | Tipo | DTO entrada | DTO salida |
|--------|----------|------|---|---|
| POST | `/api/auth/login` ⏳ | PERSONALIZADA — JPA | `LoginRequest` | `LoginResponse` |
| POST | `/api/auth/recuperar-password/solicitar` ⏳ | PERSONALIZADA — PSQL | `PasswordRecoveryRequest` | `204` |
| POST | `/api/auth/recuperar-password/restablecer` ⏳ | PERSONALIZADA — PSQL | `PasswordResetRequest` | `204` |

Login: `findByCorreoOrNumeroDocumento` + `PasswordEncoder`, emite JWT.
Recuperación: `CALL sp_solicitar_recuperacion` / `CALL sp_restablecer_password`.

```jsonc
// LoginRequest — un solo campo: acepta correo O número de documento
{
  "identifier": "1001234567",
  "password": "miClave123"
}

// LoginResponse
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 12,
  "personId": 8,                 // null si es un ADMIN sin persona
  "fullName": "Laura Gómez Restrepo",
  "roles": ["MIEMBRO"]
}

// PasswordRecoveryRequest
{ "identifier": "laura@correo.com" }

// PasswordResetRequest
{
  "token": "a3f9...",
  "newPassword": "nuevaClave123"  // el service la hashea; el SP recibe el hash ya calculado ⚙️
}
```

⚠️ `sp_solicitar_recuperacion` devuelve el token por `OUT`. **No lo
devuelvas en el response** — se envía por el canal de recuperación. El
endpoint responde `204` aunque el usuario no exista, para no revelar qué
identificadores están registrados.

No existe un endpoint separado para "crear cuenta de alumno" — toda
persona registrada nace con cuenta (ver `POST /api/personas`).

---

## Módulo `students/` (persona + contacto de emergencia)

| Método | Endpoint | Tipo | DTO entrada | DTO salida |
|--------|----------|------|---|---|
| GET | `/api/personas?buscar=` | CRUD | — | `Page<PersonResponse>` |
| GET | `/api/personas/{id}` | CRUD | — | `PersonResponse` |
| POST | `/api/personas` | PERSONALIZADA — PSQL | `PersonRequest` | `PersonResponse` |
| POST | `/api/personas/autoregistro` | PERSONALIZADA — PSQL | `PersonRequest` | `PersonResponse` |
| PUT | `/api/personas/{id}` | CRUD | `PersonUpdateRequest` | `PersonResponse` |
| GET | `/api/personas/{id}/contacto-emergencia` | CRUD | — | `EmergencyContactResponse` |
| PUT | `/api/personas/{id}/contacto-emergencia` | CRUD | `EmergencyContactRequest` | `EmergencyContactResponse` |
| PATCH | `/api/personas/{id}/desactivar` | CRUD | — | `204` |
| PATCH | `/api/personas/{id}/reactivar` | PERSONALIZADA — PSQL | — | `PersonResponse` |
| DELETE | `/api/personas/{id}` | PERSONALIZADA — PSQL | — | `204` |
| GET | `/api/personas/{id}/perfil` | VIEW | — | `PersonProfileViewResponse` |
| GET | `/api/personas/{id}/saldo` | VIEW | — | `PersonBalanceViewResponse` |

```jsonc
// PersonRequest — sirve para POST /api/personas y POST /api/personas/autoregistro
{
  "documentType": "CC",          // CC | TI | CE | PA
  "documentNumber": "1001234567",
  "firstName": "Laura",
  "lastName": "Gómez Restrepo",
  "phone": "3001234567",
  "password": null               // null → el backend hashea el documento (RF-01)
}
// source ⚙️ NO va en el body: lo fija el endpoint (ADMIN vs AUTOREGISTRO)
```

> **Por qué un solo DTO para los dos endpoints:** comparten el 100 % de
> los campos. Lo único que cambia es `fuente_registro`, y ese es
> derivado ⚙️ — lo decide la ruta, no el cliente. Si viniera en el body,
> cualquiera podría marcar un registro manual como autoregistro.

```jsonc
// PersonResponse
{
  "id": 8,
  "documentType": "CC",
  "documentNumber": "1001234567",
  "firstName": "Laura",
  "lastName": "Gómez Restrepo",
  "phone": "3001234567",
  "source": "ADMIN",             // ADMIN | AUTOREGISTRO
  "registeredAt": "2026-07-18T09:14:22Z",
  "active": true,
  "userId": 12                   // cuenta creada en la misma transacción
}

// PersonUpdateRequest — no incluye source, registeredAt ni active
{
  "documentType": "CC",
  "documentNumber": "1001234567",
  "firstName": "Laura",
  "lastName": "Gómez Restrepo",
  "phone": "3009998877"
}

// EmergencyContactRequest — el PUT crea la fila si no existía
{
  "name": "Marta Restrepo",
  "phone": "3105554433",
  "observations": "Asmática. Inhalador en el bolso."
}
// Al menos uno de los tres debe venir (chk_contacto_emergencia_no_vacio)

// EmergencyContactResponse
{
  "personId": 8,
  "name": "Marta Restrepo",
  "phone": "3105554433",
  "observations": "Asmática. Inhalador en el bolso.",
  "updatedAt": "2026-08-01T16:02:10Z"
}

// PersonProfileViewResponse — vista_perfil_persona, solo lectura
{
  "personId": 8,
  "fullName": "Laura Gómez Restrepo",
  "documentType": "CC",
  "documentNumber": "1001234567",
  "phone": "3001234567",
  "active": true,
  "emergencyContactName": "Marta Restrepo",
  "emergencyContactPhone": "3105554433",
  "emergencyObservations": "Asmática. Inhalador en el bolso.",
  "balance": 8000.00,
  "availableCreditDays": 2,
  "totalAttendances": 34,
  "lastAttendanceDate": "2026-08-25",
  "lastAttendanceTime": "2026-08-25T06:12:40Z"
}

// PersonBalanceViewResponse — vista_saldo_persona
{
  "personId": 8,
  "firstName": "Laura",
  "lastName": "Gómez Restrepo",
  "totalPaid": 40000.00,
  "totalCharged": 32000.00,
  "balance": 8000.00             // negativo = debe
}
```

`GET /api/personas` acepta `?buscar=` (nombre o documento) + paginación
estándar (`page`, `size`, `sort`). No hay filtro de eliminados: una
persona eliminada ya no existe como fila.

`DELETE` → `CALL sp_eliminar_persona(id, 🔐, force=true)`. **Irreversible**,
solo `ADMIN`. El frontend debe exigir confirmación fuerte (escribir el
nombre), no un solo click.

---

## Módulo `schedule/` (horario, excepciones, sesiones)

| Método | Endpoint | Tipo | DTO entrada | DTO salida |
|--------|----------|------|---|---|
| GET | `/api/horarios?activo=&diaSemana=` | CRUD | — | `ScheduleResponse[]` |
| GET | `/api/horarios/{id}` | CRUD | — | `ScheduleResponse` |
| POST | `/api/horarios` | CRUD | `ScheduleRequest` | `ScheduleResponse` |
| PUT | `/api/horarios/{id}` | CRUD | `ScheduleRequest` | `ScheduleResponse` |
| DELETE | `/api/horarios/{id}` | CRUD | — | `204` |
| POST | `/api/horarios/{horarioId}/excepciones` | CRUD | `ScheduleExceptionRequest` | `ScheduleExceptionResponse` |
| GET | `/api/horarios/{horarioId}/excepciones` | CRUD | — | `ScheduleExceptionResponse[]` |
| GET | `/api/horarios/excepciones?desde=&hasta=` | CRUD | — | `ScheduleExceptionResponse[]` |
| GET | `/api/horarios/excepciones/{id}` | CRUD | — | `ScheduleExceptionResponse` |
| DELETE | `/api/horarios/excepciones/{id}` | CRUD | — | `204` |
| POST | `/api/sesiones` | PERSONALIZADA — PSQL | `SessionRequest` | `SessionResponse` |
| GET | `/api/sesiones?desde=&hasta=` | CRUD | — | `Page<SessionResponse>` |
| GET | `/api/sesiones/{id}` | CRUD | — | `SessionResponse` |
| GET | `/api/sesiones/fecha/{fecha}` | CRUD | — | `SessionResponse[]` |
| DELETE | `/api/sesiones/{id}` | PERSONALIZADA — PSQL | — | `SessionResponse` |
| PUT | `/api/sesiones/{id}/cerrar` | CRUD | — | `SessionResponse` |
| PUT | `/api/sesiones/{id}/profesor/{teacherUserId}` | CRUD | — | `SessionResponse` |
| GET | `/api/usuarios/profesores` | CRUD | — | `TeacherResponse[]` |
| GET | `/api/sesiones/{id}/resumen` | VIEW | — | `SessionSummaryViewResponse` |
| GET | `/api/sesiones/resumenes/fecha/{fecha}` | VIEW | — | `SessionSummaryViewResponse[]` |
| GET | `/api/sesiones/resumenes?desde=&hasta=` | VIEW | — | `Page<SessionSummaryViewResponse>` |

```jsonc
// ScheduleRequest
{
  "dayOfWeek": 1,                // ISO: 1 = lunes ... 7 = domingo
  "startTime": "06:00",
  "endTime": "07:00"
}
// active ⚙️ nace en true; se apaga con DELETE, no por el body

// ScheduleResponse
{
  "id": 3,
  "dayOfWeek": 1,
  "startTime": "06:00",
  "endTime": "07:00",
  "active": true
}

// ScheduleExceptionRequest — "este horario no aplica esta fecha"
{
  "date": "2026-09-16",
  "reason": "Festivo"
}
// scheduleId 🔗 viene del path

// ScheduleExceptionResponse
{ "id": 5, "scheduleId": 3, "date": "2026-09-16", "reason": "Festivo" }

// SessionRequest — CALL sp_crear_sesion_manual
{
  "date": "2026-08-27",
  "startTime": "2026-08-27T06:00:00Z",
  "endTime": "2026-08-27T07:00:00Z",   // null si sigue en curso
  "teacherUserId": 12,                 // null = sin asignar; si viene, debe tener rol PROFESOR
  "scheduleId": null                   // null = sesión suelta (taller puntual)
}

// SessionResponse
{
  "id": 41,
  "scheduleId": null,
  "date": "2026-08-27",
  "startTime": "2026-08-27T06:00:00Z",
  "endTime": null,
  "teacherUserId": 12,
  "cancelled": false
}

// TeacherResponse — puebla el select de "profesor" al crear una sesión
{ "userId": 12, "personId": 8, "fullName": "Laura Gómez Restrepo" }

// SessionSummaryViewResponse — vista_resumen_sesiones
{
  "sessionId": 41,
  "scheduleId": null,
  "date": "2026-08-27",
  "startTime": "2026-08-27T06:00:00Z",
  "endTime": null,
  "teacherUserId": 12,
  "teacherName": "Laura Gómez Restrepo",   // "Sin asignar" si no hay profesor
  "cancelled": false,
  "totalCheckins": 18,
  "totalAttended": 15
}
```

`DELETE /api/sesiones/{id}` → `CALL sp_cancelar_sesion(id, 🔐)`. No borra la fila: marca
`cancelada = true` y lo deja auditado. Distinto de declarar una excepción de horario (esa
evita que la sesión llegue a existir).

`PUT /api/sesiones/{id}/profesor/{teacherUserId}` no lleva body: los dos datos van en el
path, así que no hace falta un DTO de entrada.

**Criterio de rutas:** un filtro obligatorio de una sola clave va en el path
(`/fecha/{fecha}`, `/persona/{id}`); un filtro opcional o combinable va en query
(`?activo=`, `?desde=&hasta=`). Por eso conviven las dos formas.

`GET /api/horarios` acepta `?activo=true` y `?diaSemana=1` (ISO: 1 = lunes … 7 = domingo).
Sin parámetros devuelve todos, activos e inactivos.

`GET /api/sesiones` exige `?desde=&hasta=` y va paginado; para un solo día está
`/api/sesiones/fecha/{fecha}`, que devuelve un array porque un día tiene una o dos sesiones.

---

## Módulo `attendance/` (llegada + asistencia)

| Método | Endpoint | Tipo | DTO entrada | DTO salida |
|--------|----------|------|---|---|
| POST | `/api/checkin/qr` ⚠️`75001` ⚠️`75002` | PERSONALIZADA — PSQL | `CheckinRequest` | `CheckinResponse` |
| POST | `/api/checkin/documento` ⚠️`75001` ⚠️`75002` | PERSONALIZADA — PSQL | `CheckinByDocumentRequest` | `CheckinResponse` |
| POST | `/api/checkin/manual` ⚠️`75001` ⚠️`75002` | PERSONALIZADA — PSQL | `CheckinRequest` | `CheckinResponse` |
| GET | `/api/checkin/{id}` | CRUD | — | `CheckinResponse` |
| GET | `/api/checkin/sesion/{sessionId}` | CRUD | — | `CheckinResponse[]` |
| GET | `/api/checkin/fecha/{fecha}` | CRUD | — | `CheckinResponse[]` |
| GET | `/api/checkin/persona/{personId}` | CRUD | — | `Page<CheckinResponse>` |
| PUT | `/api/llegadas/{id}/asistencia` | PERSONALIZADA — PSQL | **sin body** | `AttendanceResponse` |
| DELETE | `/api/llegadas/{id}/asistencia` | PERSONALIZADA — PSQL | — | `204` |
| GET | `/api/llegadas/{id}/asistencia` | CRUD | — | `AttendanceResponse` |
| GET | `/api/asistencias/historial/persona/{personaId}` | VIEW | — | `Page<AttendanceHistoryViewResponse>` |
| GET | `/api/asistencias/historial/sesion/{sesionId}` | VIEW | — | `Page<AttendanceHistoryViewResponse>` |
| GET | `/api/asistencias/historial?desde=&hasta=` | VIEW | — | `Page<AttendanceHistoryViewResponse>` |
| GET | `/api/personas/{id}/saldo-asistencias` | VIEW | — | `Page<AttendanceBalanceViewResponse>` |

```jsonc
// CheckinRequest — /qr y /manual
{ "personId": 8 }

// CheckinByDocumentRequest — kiosko: la persona digita su documento, no conoce su id
{ "documentNumber": "1001234567" }
// 404 si la persona no existe → el kiosko ofrece registrarla

// CheckinResponse — los 3 endpoints de check-in devuelven esto
{
  "checkinId": 77,
  "personId": 8,
  "sessionId": 41,
  "fullName": "Laura Gómez Restrepo",
  "arrivalTime": "2026-08-27T05:48:31Z",
  "checkinMethod": "QR",         // QR | DOCUMENTO | MANUAL
  "queuePosition": 4             // ⚙️ leído de vista_fila_hoy tras el CALL
}
```

> `sp_registrar_llegada` solo devuelve `p_llegada_id` por `OUT`. El
> resto del `CheckinResponse` (nombre, sesión, posición) sale de una
> lectura posterior a `vista_fila_hoy` dentro de la misma transacción —
> el procedimiento no los devuelve.

```jsonc
// PUT /api/llegadas/{id}/asistencia — NO LLEVA BODY
// checkinId    🔗 path
// markedByUser 🔐 JWT
// amountCharged ⚙️ la calcula fn_tarifa_vigente() dentro del SP

// AttendanceResponse
{
  "attendanceId": 55,
  "checkinId": 77,
  "personId": 8,
  "fullName": "Laura Gómez Restrepo",
  "amountCharged": 4000.00,
  "markedByUserId": 3,
  "markedByName": "Camilo Pérez",
  "markedAt": "2026-08-27T06:02:11Z"
}
```

> **No existe `AttendanceRequest`.** Confirmar una asistencia no recibe
> ningún dato del cliente: el id va en la URL, el usuario en el JWT y el
> monto lo pone la base de datos. Un `Request` aquí solo puede llevar
> campos que el backend va a ignorar.
>
> **`amountCharged` no es de entrada.** `sp_registrar_asistencia`
> recibe únicamente `(llegada_id, usuario_id)`; el monto lo resuelve
> internamente con `fn_tarifa_vigente()`. Para responderlo, el service
> lee la fila de `asistencia` recién creada después del `CALL`.

```jsonc
// AttendanceHistoryViewResponse — vista_historial_asistencias
{
  "attendanceId": 55,
  "checkinId": 77,
  "sessionId": 41,
  "personId": 8,
  "fullName": "Laura Gómez Restrepo",   // "Persona eliminada" si fue purgada
  "date": "2026-08-27",
  "arrivalTime": "2026-08-27T05:48:31Z",
  "checkinMethod": "QR",
  "amountCharged": 4000.00,
  "markedAt": "2026-08-27T06:02:11Z",
  "markedByUserId": 3,
  "markedByName": "Camilo Pérez",
  "paymentMethod": "EFECTIVO"           // null si no hubo movimiento ligado
}

// AttendanceBalanceViewResponse — vista_saldo_por_asistencia
{
  "attendanceId": 55,
  "personId": 8,
  "date": "2026-08-27",
  "amountCharged": 4000.00,
  "balanceAfterThisAttendance": 4000.00,
  "balanceStatus": "CUBIERTA",   // CUBIERTA | PARCIAL | DEBE
  "pendingAmount": 0.00
}
```

El historial se consulta por tres rutas distintas, todas paginadas:
`/historial/persona/{personaId}`, `/historial/sesion/{sesionId}` y
`/historial?desde=&hasta=`.

**Nota de diseño para el frontend admin:** confirmar asistencia y
registrar pago son **dos acciones separadas** en la API — no existe un
"marcar pagado" que haga las dos juntas. La UI necesita dos controles
independientes.

---

## Módulo `queue/` (cola)

| Método | Endpoint | Tipo | DTO entrada | DTO salida |
|--------|----------|------|---|---|
| GET | `/api/cola/admin?sesionId=` | VIEW | — | `TodayQueueViewResponse[]` |
| GET | `/api/cola/pendientes?sesionId=` | VIEW | — | `TodayQueueViewResponse[]` |
| GET | `/api/cola/espera` | VIEW | — | `WaitingQueueViewResponse[]` |
| GET | `/api/cola/prepagada` | VIEW | — | `PrepaidQueueViewResponse[]` |
| GET | `/api/cola/configuracion` | CRUD | — | `QueueConfigResponse` |
| PUT | `/api/cola/configuracion` | CRUD | `QueueConfigRequest` | `QueueConfigResponse` |

```jsonc
// TodayQueueViewResponse — vista_fila_hoy. Pantalla admin: sí lleva dinero.
{
  "checkinId": 77,
  "sessionId": 41,
  "personId": 8,
  "firstName": "Laura",
  "lastName": "Gómez Restrepo",
  "arrivalTime": "2026-08-27T05:48:31Z",
  "checkinMethod": "QR",
  "position": 4,
  "attendanceId": 55,            // null si aún no se confirma
  "amountCharged": 4000.00,      // null si aún no se confirma
  "markedAt": "2026-08-27T06:02:11Z",
  "attended": true,
  "balance": 8000.00,
  "note": "Pagó 4 días por adelantado"
}

// WaitingQueueViewResponse — vista_fila_espera. Pantalla TV: CERO datos de dinero (RNF-03).
{
  "checkinId": 77,
  "sessionId": 41,
  "firstName": "Laura",
  "lastName": "Gómez Restrepo",
  "arrivalTime": "2026-08-27T05:48:31Z",
  "position": 4
}

// PrepaidQueueViewResponse — vista_fila_prepagada. Igual, sin dinero ni posición.
{
  "checkinId": 77,
  "firstName": "Diana",
  "lastName": "Ortiz",
  "arrivalTime": "2026-08-27T05:50:02Z"
}

// QueueConfigRequest
{
  "peoplePerTurn": 1,
  "entryMarginMinutes": 60,
  "paymentVisibilityMinutes": 2
}

// QueueConfigResponse
{
  "peoplePerTurn": 1,
  "entryMarginMinutes": 60,
  "paymentVisibilityMinutes": 2,
  "updatedAt": "2026-08-20T11:00:00Z",
  "updatedByUserId": 3
}
```

> `TodayQueueViewResponse` y `WaitingQueueViewResponse` comparten ~45 % de los campos,
> pero son **DTOs dedicados a propósito**: la vista de TV no debe poder exponer saldo ni
> monto aunque alguien se equivoque en el controller. La separación es una barrera de
> privacidad, no una duplicación.

El backend **solo expone** la lista y el valor de `peoplePerTurn` — qué
filas se resaltan como "les toca pasar" es lógica exclusiva del frontend
de la pantalla de espera.

---

## Módulo `payments/` (pagos, métodos, tarifa)

| Método | Endpoint | Tipo | DTO entrada | DTO salida |
|--------|----------|------|---|---|
| GET | `/api/metodos-pago?activo=` | CRUD | — | `PaymentMethodResponse[]` |
| GET | `/api/metodos-pago/{id}` | CRUD | — | `PaymentMethodResponse` |
| POST | `/api/metodos-pago` | CRUD | `PaymentMethodRequest` | `PaymentMethodResponse` |
| PUT | `/api/metodos-pago/{id}` | CRUD | `PaymentMethodRequest` | `PaymentMethodResponse` |
| POST | `/api/movimientos` ⚠️`75001` | PERSONALIZADA — PSQL | `AccountMovementRequest` | `AccountMovementResponse` |
| PATCH | `/api/movimientos/{id}` | CRUD | `AccountMovementPatchRequest` | `AccountMovementResponse` |
| POST | `/api/movimientos/{id}/reembolso` | PERSONALIZADA — PSQL | `RefundRequest` | `AccountMovementResponse` |
| POST | `/api/movimientos/{id}/vuelto` | PERSONALIZADA — PSQL | — | `AccountMovementResponse` |
| GET | `/api/movimientos/{id}` | CRUD | — | `AccountMovementResponse` |
| GET | `/api/movimientos/persona/{personId}` | CRUD | — | `Page<AccountMovementResponse>` |
| GET | `/api/movimientos/checkin/{checkinId}` | CRUD | — | `AccountMovementResponse[]` |
| GET | `/api/movimientos/pendientes/vueltos` | VIEW | — | `PendingChangeViewResponse[]` |
| GET | `/api/movimientos/pendientes/vueltos/persona/{personId}` | VIEW | — | `PendingChangeViewResponse[]` |
| GET | `/api/reportes/recaudo-diario/{fecha}` | VIEW | — | `DailyRevenueViewResponse[]` |
| GET | `/api/reportes/recaudo-diario?desde=&hasta=` | VIEW | — | `DailyRevenueViewResponse[]` |
| GET | `/api/reportes/recaudo-por-sesion/{sesionId}` | VIEW | — | `SessionRevenueViewResponse` |
| GET | `/api/reportes/recaudo-por-sesion?desde=&hasta=` | VIEW | — | `SessionRevenueViewResponse[]` |
| GET | `/api/tarifa` | CRUD | — | `PricingResponse` |
| PUT | `/api/tarifa` | CRUD | `PricingRequest` | `PricingResponse` |
| GET | `/api/auditoria/historial-cambios?desde=&hasta=` | VIEW | — | `Page<ChangeHistoryViewResponse>` |
| GET | `/api/auditoria/historial-cambios/registro/{tabla}/{registroId}` | VIEW | — | `Page<ChangeHistoryViewResponse>` |
| GET | `/api/auditoria/historial-cambios/usuario/{usuarioId}` | VIEW | — | `Page<ChangeHistoryViewResponse>` |

```jsonc
// PaymentMethodRequest
{ "name": "NEQUI", "active": true }

// PaymentMethodResponse
{ "id": 3, "name": "NEQUI", "active": true }

// AccountMovementRequest — CALL sp_registrar_movimiento
{
  "personId": 8,
  "checkinId": 77,               // null: pago por adelantado, sin llegada asociada
  "amountReceived": 5000.00,     // lo que entregó la persona
  "amountApplied": 4000.00,      // lo que se abona a su cuenta (el resto es vuelto)
  "paymentMethodId": 1,
  "note": "Pagó 4 días por adelantado"
}
// userId 🔐 JWT — quién registró el movimiento

// AccountMovementResponse
{
  "id": 120,
  "personId": 8,
  "checkinId": 77,
  "amountReceived": 5000.00,
  "amountApplied": 4000.00,
  "paymentMethodId": 1,
  "paymentMethodName": "EFECTIVO",
  "movementType": "ABONO",       // ABONO | REEMBOLSO
  "originalMovementId": null,
  "changeDelivered": false,
  "dateTime": "2026-08-27T06:05:00Z",
  "userId": 3,
  "note": "Pagó 4 días por adelantado"
}

// AccountMovementPatchRequest — corregir un error de digitación. Todos opcionales.
{
  "amountReceived": 4000.00,
  "amountApplied": 4000.00,
  "paymentMethodId": 2,
  "note": null
}

// RefundRequest — devolución real de dinero, distinta de corregir un typo
{
  "amount": 4000.00,             // positivo; el SP lo guarda negativo. Tope: el monto original
  "paymentMethodId": 1,
  "note": "No alcanzó a entrar a la clase"
}
// movementId 🔐/🔗 el original va en el path; userId del JWT
```

> `AccountMovementRequest` y `RefundRequest` comparten ~35 % de sus
> campos → **DTOs dedicados**. Además el reembolso no recibe `personId`
> ni `checkinId`: los hereda del movimiento original.

```jsonc
// PendingChangeViewResponse — vista_vueltos_pendientes
{
  "movementId": 120,
  "personId": 8,
  "fullName": "Laura Gómez Restrepo",
  "amountReceived": 5000.00,
  "amountApplied": 4000.00,
  "change": 1000.00,
  "dateTime": "2026-08-27T06:05:00Z"
}

// DailyRevenueViewResponse — vista_recaudo_diario
{ "date": "2026-08-27", "sessionId": 41, "totalCollected": 72000.00 }

// SessionRevenueViewResponse — vista_recaudo_por_sesion
{ "date": "2026-08-27", "sessionId": 41, "totalCollected": 72000.00 }

// PricingRequest
{ "amount": 4500.00 }

// PricingResponse
{
  "id": 1,
  "amount": 4500.00,
  "updatedAt": "2026-08-27T08:00:00Z",
  "updatedByUserId": 3,
  "updatedByName": "Camilo Pérez"
}

// ChangeHistoryViewResponse — vista_historial_cambios
{
  "id": 300,
  "affectedTable": "TARIFA",     // ASISTENCIA | MOVIMIENTO_CUENTA | TARIFA | CONFIGURACION_COLA | PERSONA | SESION
  "recordId": 1,
  "field": "monto",
  "oldValue": "4000.00",
  "newValue": "4500.00",
  "userId": 3,
  "performedBy": "Camilo Pérez",
  "dateTime": "2026-08-27T08:00:00Z"
}
```

`DailyRevenueViewResponse` y `SessionRevenueViewResponse` son idénticos en forma: cambia
la agregación en SQL, no el contrato. Están separados porque cada uno mapea su propia
vista y su propia entidad `@Immutable`; unificarlos obligaría a que dos vistas compartan
un mismo DTO, y el nombre dejaría de decir de dónde salen los datos.

---

## Módulo `websocket/`

No aplica la clasificación CRUD/VIEW/PSQL — no es REST.

- **`cola:actualizada`** — se emite tras un check-in nuevo, una
  confirmación de asistencia, o una anulación. Lo publica el **Service**
  al final de su transacción, no un trigger de Postgres ni un poller.
  Payload: el `TodayQueueViewResponse[]` completo de la sesión vigente, que es lo mismo
  que devuelve `GET /api/cola/admin?sesionId=`.
- **`cola:configuracion-actualizada`** — al cambiar `peoplePerTurn`.
  Payload: `QueueConfigResponse`.

WebSocket nunca se usa para operaciones CRUD normales.

---

## Resumen de decisiones que afectan al frontend

1. **Asistencia y pago son independientes** — dos endpoints, dos
   acciones, en cualquier orden.
2. **El cliente nunca elige sesión** — el backend la resuelve por
   horario y hora actual.
3. **`paymentMethodId` es un entero**, no un string de enum — el select
   se puebla desde `GET /api/metodos-pago`.
4. **El frontend nunca envía el id del usuario que ejecuta la acción.**
   Sale del JWT 🔐. Si un endpoint parece necesitarlo en el body, está
   mal diseñado.
5. **El frontend nunca envía montos que la base de datos calcula.**
   `amountCharged` de una asistencia es el caso típico: se recibe en el
   response, nunca se manda en el request.
