# Flujo de datos — PSR Check-In

Recorrido completo de un dato, desde el navegador hasta PostgreSQL y de vuelta.
Complementa a `PSR_Check-In_Endpoints.md` (el contrato de la API) y a
`PSR_Check-In_Guia_Persistencia.md` (cómo acceder a la base).

**Este documento describe el código tal como está**, no el diseño previsto. Donde ambos
difieren, está anotado en §9.

---

## 1. Las capas

```
React
  │  JSON + JWT en la cabecera
  ▼
Controller ──── @Valid sobre el body, saca el userId del JWT, traduce path y query
  │  DTO Request + ids sueltos
  ▼
Service ─────── abre la transacción, orquesta, aplica lo que la BD no cubre
  │  entidades / primitivos
  ▼
Repository ──── solo acceso a datos, en cuatro sabores (§2)
  │  JPQL / CALL / SELECT sobre vista
  ▼
PostgreSQL ──── procedimientos, funciones, triggers, vistas, restricciones
```

**La regla que atraviesa todo el proyecto:** el id de quien ejecuta la acción **nunca
viaja en el body**. El controller lo saca del JWT y lo pasa como parámetro
(`executedByUserId`, `markedByUserId`, `registeredByUserId`, `updatedByUserId`). Si
viniera del cliente, cualquiera podría registrar acciones a nombre de otro.

Y la segunda: **el cliente nunca elige la sesión**. La resuelve la base de datos.

---

## 2. Los cuatro caminos de un repositorio

| Camino | Clase | Cuándo se usa |
|---|---|---|
| **JPA** | `XxxRepository extends JpaRepository` | CRUD sin lógica |
| **Procedimiento** | `XxxProcedureRepository` (clase, no interfaz) | Lo que ya resuelve un `sp_` |
| **Vista** | `XxxViewRepository extends Repository` | Lo que ya resuelve un `vista_` |
| **Auditoría** | `AuditContextRepository` | Publicar `app.usuario_id` antes de un UPDATE auditado |

Los de procedimientos son clases porque `SimpleJdbcCall` es código ejecutable y no cabe en
una interfaz. Los de vistas extienden `Repository` y no `JpaRepository` para no exponer
`save()` ni `delete()` sobre algo de solo lectura.

**Antes de escribir un método nuevo:** si existe como procedimiento, se llama; si existe
como vista, se lee. No se reconstruye con JPA aunque parezca un INSERT simple.

---

## 3. Mapa completo: endpoint → service → repositorio → SQL

### 3.1 Check-in — `CheckinController` (`/api/checkin`)

| Ruta | Service | Repositorio | Objeto SQL |
|---|---|---|---|
| `POST /qr` | `register(req, QR)` | `CheckinProcedureRepository` | `sp_registrar_llegada` |
| `POST /manual` | `register(req, MANUAL)` | idem | idem |
| `POST /documento` | `registerByDocument(req)` | `PersonRepository` + procedimiento | `sp_registrar_llegada` |
| `GET /{id}` | `findById` | `CheckinRepository` | tabla `llegada` |
| `GET /sesion/{id}` | `findBySession` | idem | idem |
| `GET /fecha/{date}` | `findByDate` | idem | idem |
| `GET /persona/{id}` | `findByPerson` | idem | idem |

### 3.2 Asistencia — `AttendanceController`

| Ruta | Service | Repositorio | Objeto SQL |
|---|---|---|---|
| `PUT /api/llegadas/{id}/asistencia` | `confirm(id, 🔐)` | `AttendanceProcedureRepository` | `sp_registrar_asistencia` |
| `DELETE /api/llegadas/{id}/asistencia` | `cancel(id)` | idem | `sp_anular_asistencia` |
| `GET /api/llegadas/{id}/asistencia` | `findByCheckinId` | `AttendanceRepository` | tabla `asistencia` |
| `GET /api/asistencias/historial/persona/{id}` | `getHistoryByPerson` | `AttendanceHistoryViewRepository` | `vista_historial_asistencias` |
| `GET /api/asistencias/historial/sesion/{id}` | `getHistoryBySession` | idem | idem |
| `GET /api/asistencias/historial` | `getHistoryBetween` | idem | idem |
| `GET /api/personas/{id}/saldo-asistencias` | `getBalanceByPerson` | `AttendanceBalanceViewRepository` | `vista_saldo_por_asistencia` |

### 3.3 Cola — `QueueController` y `QueueConfigController`

| Ruta | Service | Repositorio | Objeto SQL |
|---|---|---|---|
| `GET /api/cola/admin` | `getTodayQueue` | `TodayQueueViewRepository` | `vista_fila_hoy` |
| `GET /api/cola/pendientes` | `getPendingBySession` | idem | idem |
| `GET /api/cola/espera` | `getWaitingQueue` | `WaitingQueueViewRepository` | `vista_fila_espera` |
| `GET /api/cola/prepagada` | `getPrepaidQueue` | `PrepaidQueueViewRepository` | `vista_fila_prepagada` |
| `GET /api/cola/configuracion` | `getCurrent` | `QueueConfigRepository` | tabla `configuracion_cola` |
| `PUT /api/cola/configuracion` | `update(req, 🔐)` | `QueueConfigRepository` + `AuditContextRepository` | UPDATE + trigger |

### 3.4 Dinero — `AccountMovementController` (`/api/movimientos`)

| Ruta | Service | Repositorio | Objeto SQL |
|---|---|---|---|
| `POST /` | `registerDeposit(req, 🔐)` | `AccountMovementProcedureRepository` | `sp_registrar_movimiento` |
| `POST /{id}/reembolso` | `registerRefund` | idem | `sp_registrar_reembolso` |
| `POST /{id}/vuelto` | `deliverChange(id, 🔐)` | idem (+ `set_config`) | `sp_entregar_vuelto` |
| `PATCH /{id}` | `correct(id, req, 🔐)` | `AccountMovementRepository` + `AuditContextRepository` | UPDATE + trigger |
| `GET /{id}` · `/persona/{id}` · `/checkin/{id}` | `findBy…` | `AccountMovementRepository` | tabla `movimiento_cuenta` |
| `GET /pendientes/vueltos[/persona/{id}]` | `findPendingChanges…` | `PendingChangeViewRepository` | `vista_vueltos_pendientes` |

### 3.5 Métodos de pago y tarifa

| Ruta | Service | Repositorio | Objeto SQL |
|---|---|---|---|
| `GET·POST·PUT /api/metodos-pago` | `IPaymentMethodService` | `PaymentMethodRepository` | tabla `metodo_pago` |
| `GET /api/tarifa` | `getCurrent` | `PricingRepository` | tabla `tarifa` (fila id = 1) |
| `PUT /api/tarifa` | `update(req, 🔐)` | `PricingRepository` + `AuditContextRepository` | UPDATE + trigger |

La tarifa también se lee sola desde SQL: `fn_tarifa_vigente()` la consulta dentro de
`sp_registrar_asistencia` y de `vista_fila_prepagada`.

### 3.6 Horarios, excepciones y sesiones

| Ruta | Service | Repositorio | Objeto SQL |
|---|---|---|---|
| `GET·POST·PUT /api/horarios` | `IScheduleService` | `ScheduleRepository` | tabla `horario` |
| `DELETE /api/horarios/{id}` | `setActive(id, false)` | idem | **desactiva**, no borra |
| `POST·GET /api/horarios/{id}/excepciones` | `IScheduleExceptionService` | `ScheduleExceptionRepository` | tabla `horario_excepcion` |
| `GET·DELETE /api/horarios/excepciones/{id}` | idem | idem | idem |
| `POST /api/sesiones` | `createManual` | `SessionProcedureRepository` | `sp_crear_sesion_manual` |
| `DELETE /api/sesiones/{id}` | `cancel(id, 🔐)` | idem | `sp_cancelar_sesion` |
| `PUT /api/sesiones/{id}/cerrar` | `close` | `SessionRepository` | UPDATE `hora_fin` |
| `PUT /api/sesiones/{id}/profesor/{userId}` | `assignTeacher` | idem | UPDATE |
| `GET /api/sesiones[/{id}][/fecha/{d}]` | `findBy…` | idem | tabla `sesion` |
| `GET /api/sesiones/{id}/resumen` · `/resumenes…` | `getSummary…` | `SessionSummaryViewRepository` | `vista_resumen_sesiones` |

Un horario nunca se borra físicamente: hay sesiones que lo referencian.

### 3.7 Roles

| Ruta | Service | Repositorio | Objeto SQL |
|---|---|---|---|
| `GET /api/roles[/{id}][/nombre/{n}]` | `IRoleService` | `RoleRepository` | tabla `rol` (semilla fija) |
| `POST /api/usuarios/{id}/roles` | `assign` | `UserRoleProcedureRepository` | `sp_asignar_rol` |
| `DELETE /api/usuarios/{id}/roles/{rol}` | `revoke` | idem | `sp_quitar_rol` |
| `GET /api/usuarios/{id}/roles` | `findByUser` | `UserRoleRepository` | tabla `usuario_rol` |

Un usuario tiene **varios roles a la vez**: las authorities se cargan como colección desde
`usuario_rol`, no de una columna en `usuario`.

### 3.8 Auditoría — `ChangeHistoryController` (`/api/auditoria`)

Solo lectura, siempre sobre `vista_historial_cambios`, que ya resuelve el nombre de quien
hizo el cambio. Tres filtros: por registro (`/registro/{tabla}/{id}`), por usuario y por
rango de fechas.

### 3.9 Reportes — `ReportController` (`/api/reportes`)

`recaudo-diario[/{fecha}]` sobre `vista_recaudo_diario` y
`recaudo-por-sesion[/{sesionId}]` sobre `vista_recaudo_por_sesion`. Cero lógica en Java:
la agregación es SQL.

### 3.10 Persona — `PersonController` y `EmergencyContactController` (`/api/personas`)

| Ruta | Service | Repositorio | Objeto SQL |
|---|---|---|---|
| `POST /` · `POST /autoregistro` | `RegisterService.registerByAdmin` · `.registerSelf` | `PersonProcedureRepository` + `IUserService` + `IInvitationService` | `sp_registrar_persona` + `sp_registrar_usuario_para_persona` |
| `GET /?buscar=` | `findAll` / `search` | `PersonRepository` (+ `UserRepository` para el `userId`) | tabla `persona` |
| `GET /{id}` | `findById` | idem | idem |
| `PUT /{id}` | `update` | `PersonRepository` | UPDATE |
| `PATCH /{id}/desactivar` | `deactivate` | idem | UPDATE `activo` |
| `PATCH /{id}/reactivar` | `reactivate(id, 🔐)` | `PersonProcedureRepository` | `sp_reactivar_persona` |
| `DELETE /{id}` | `deletePermanently(id, 🔐)` | idem | `sp_eliminar_persona` |
| `GET /{id}/perfil` | `getProfile` | `PersonProfileViewRepository` | `vista_perfil_persona` |
| `GET /{id}/saldo` | `getBalance` | `PersonBalanceViewRepository` | `vista_saldo_persona` |
| `GET·PUT /{id}/contacto-emergencia` | `IEmergencyContactService` | `EmergencyContactRepository` | tabla `contacto_emergencia` |

El `PUT` del contacto de emergencia es un upsert: crea la fila si la persona no tenía.

### 3.11 Usuario — `UserController` (`/api/usuarios`)

| Ruta | Service | Repositorio | Objeto SQL |
|---|---|---|---|
| `GET /profesores` | `findAssignableTeachers` | `UserRepository.findActiveByRole` | JPQL sobre `usuario_rol` |
| `GET /` · `/{id}` · `/persona/{personId}` | `findAll` / `findById` / `findByPersonId` | `UserRepository` | tabla `usuario` |
| `PATCH /{id}/activar` · `/desactivar` | `setActive` | idem | UPDATE `activo` |

No expone alta: la cuenta nace con la persona. Tampoco cambio de contraseña, que entra por
auth con su token.

### 3.12 Auth — **pendiente**

Login, solicitar recuperación y restablecer contraseña. Los procedimientos ya existen
(`sp_solicitar_recuperacion`, `sp_restablecer_password`); falta el módulo.

---

## 4. Los flujos que hacen algo más que leer o escribir

### 4.1 Alta de persona

Dos procedimientos en **una sola transacción**: si falla el segundo, la persona tampoco
queda registrada.

```
PersonRequest ─► RegisterService.registerByAdmin(...) | .registerSelf(...)
                   │
                   ├─ 1. PersonService.createPerson(request, ADMIN | AUTOREGISTRO)
                   │       ├─ ¿documento duplicado? → 409 antes de tocar la BD
                   │       └─ CALL sp_registrar_persona(...)          ──► OUT persona_id
                   ├─ 2. UserService.createForPerson(personaId, clave, correo)
                   │       ├─ ¿correo duplicado? → 409
                   │       ├─ PasswordEncoder.encode(...)   (el hash se calcula en Java)
                   │       └─ CALL sp_registrar_usuario_para_persona ──► OUT usuario_id
                   │            └─ inserta también el rol MIEMBRO
                   ├─ 3. InvitationService.invite(usuarioId)   (solo si hay correo)
                   │       ├─ emite el token de invitación (48 h)
                   │       └─ publica el evento; el correo sale DESPUÉS del commit
                   └─ 4. relee la persona y arma PersonResponse
```

Quien coordina es **`RegisterService`**, no `PersonService`: es el único punto donde persona, cuenta y
correo ocurren en la misma transacción. `PersonService` solo sabe de personas, y `UserService` solo
toma los datos, crea el usuario y hashea.

`source` lo decide **la ruta**, no el cliente. Sin contraseña en el request, la inicial es
el propio número de documento (RF-01). El correo es opcional: sin él la cuenta se identifica solo por
documento, que es el caso normal del kiosko.

El envío del correo va en `@TransactionalEventListener(AFTER_COMMIT)` con try/catch, así que un SMTP
caído no tumba el alta — la persona ya puede entrar con su número de documento.

**Por qué se relee en el paso 4:** el procedimiento solo devuelve el id por `OUT`.
`registeredAt`, `active` y `source` los pone la base.

### 4.2 Check-in — donde más trabajo hace la base de datos

```
CheckinRequest ─► CheckinService.register(request, QR)
                    │
                    └─ CALL sp_registrar_llegada(personaId, metodo)  ──► OUT llegada_id
                         │
                         ├─ fn_validar_persona_activa     ──► SQLSTATE 75001 si desactivada
                         │
                         ├─ fn_obtener_o_crear_sesion_del_momento
                         │    ├─ fn_sesion_abierta_ahora        ¿hay sesión en curso?
                         │    └─ fn_horario_vigente_ahora       ¿qué horario aplica?
                         │         ├─ usa minutos_margen_ingreso de configuracion_cola
                         │         ├─ respeta horario_excepcion
                         │         └─ ──► SQLSTATE 75002 si no hay clase ahora
                         │
                         └─ si la persona ya estaba en la fila, devuelve su llegada
                            existente en vez de duplicarla
```

Después del `CALL`, el service lee `vista_fila_hoy` **en la misma transacción** para
completar el response con el nombre y la posición en la fila.

Los dos `SQLSTATE` no los traduce el service: los captura `GlobalExceptionHandler` por
código —nunca por el texto— y los convierte en `409` con `code` distinto
(`PERSON_DEACTIVATED` y `NO_ACTIVE_SESSION`), porque llevan a acciones distintas en
pantalla.

### 4.3 Confirmar asistencia

```
(sin body) ─► AttendanceService.confirm(checkinId 🔗, markedByUserId 🔐)
                ├─ CALL sp_registrar_asistencia(llegadaId, usuarioId)
                │    └─ el monto lo pone fn_tarifa_vigente(), no la aplicación
                └─ relee la asistencia para armar el response
```

No recibe nada del cliente: id en la URL, usuario en el JWT, monto de la base. Por eso no
existe un `AttendanceRequest`.

### 4.4 Cierre automático de sesión — el único flujo sin petición HTTP

```
cada 15 min ─► SessionClosingScheduler
                 └─ CALL sp_cerrar_sesiones_vencidas(2)
                      ├─ sesiones con hora_fin NULL, no canceladas, cuyo fin previsto
                      │  ya pasó hace más de 2 horas
                      │     fin previsto = hora_fin del horario, o inicio + 1h si es suelta
                      └─ pone hora_fin = now() y audita como usuario SISTEMA
```

Solo alcanza a las sesiones manuales creadas sin hora de fin. Es la red de seguridad para
el olvido del admin; el cierre normal es `PUT /api/sesiones/{id}/cerrar`.

### 4.5 Eliminar una persona

Irreversible. `sp_eliminar_persona` reasigna la huella al sentinel antes de borrar:

```
llegada.persona_id, movimiento_cuenta.persona_id      ──► persona sentinel (999999999)
asistencia.marcado_por, historial_cambio.usuario_id,
sesion.profesor_usuario_id, tarifa.actualizado_por,
configuracion_cola.actualizado_por                     ──► usuario sentinel (999999999)
contacto_emergencia, token_recuperacion, usuario       ──► DELETE
persona                                                ──► DELETE
```

El hecho permanece (la clase ocurrió, el dinero entró); la identidad desaparece. Por eso
los reportes históricos siguen cuadrando después de un borrado.

---

## 5. `set_config`: cuándo sí y cuándo no

Los triggers de auditoría leen `app.usuario_id` a través de `fn_usuario_actual()`. Si no
está puesto en la transacción, la operación **falla** — es intencional.

| Operación | ¿`set_config`? | Por qué |
|---|---|---|
| `sp_registrar_movimiento`, `sp_registrar_reembolso` | No | Son INSERT; el trigger es `AFTER UPDATE` |
| `sp_registrar_asistencia` | No | Ídem |
| **`sp_entregar_vuelto`** | **Sí** | Es UPDATE sobre `movimiento_cuenta` |
| Corregir un movimiento o un monto cobrado (JPA) | **Sí** | UPDATE auditado |
| Cambiar tarifa o configuración de cola (JPA) | **Sí** | UPDATE auditado |
| `sp_reactivar_persona`, `sp_cancelar_sesion` | No | Auditan solos, con el usuario que reciben |
| `sp_eliminar_persona` | No | Lo publica él mismo, internamente |

`sp_entregar_vuelto` es el caso traicionero: solo cambia un booleano, pero
`fn_trg_auditar_movimiento_cuenta` llama a `fn_usuario_actual()` en su primera línea,
antes de comparar campos. Por eso `AccountMovementProcedureRepository.deliverChange()`
publica el usuario él mismo antes del `CALL`.

---

## 6. Quién escribe en `historial_cambio`

Nunca la capa de servicio. Tres fuentes, y solo tres:

| Fuente | Qué audita | De dónde saca el usuario |
|---|---|---|
| **Triggers** | UPDATE de `movimiento_cuenta`, `asistencia`, `tarifa`, `configuracion_cola` | `app.usuario_id` |
| **Procedimientos** | reactivar y eliminar persona, cancelar sesión | Parámetro recibido |
| **Scheduler** | cierre automático de sesiones | Usuario SISTEMA |

Los dos usuarios especiales **no son intercambiables**: `999999998` es **SISTEMA**
(acciones automáticas), `999999999` es el **sentinel** (personas eliminadas). Ninguno
puede iniciar sesión.

---

## 7. Transacciones y arranque

- Todo método de service que escribe va con `@Transactional`; los de lectura, con
  `readOnly = true`.
- `JdbcTemplate` y JPA comparten `DataSource` y transacción: un `CALL` seguido de un
  `findById` ven lo mismo, sin `flush` manual.
- `set_config(..., true)` limita el valor a la transacción, así que no se filtra entre
  peticiones aunque la conexión vuelva al pool.
- **Al arrancar**, Flyway aplica primero las versionadas (`V1`, `V2`) y después las
  repetibles en orden alfabético: `FUNCTIONS` → `STORED_PROCEDURES` → `TRIGGERS` →
  `VIEWS`. Ese orden importa: las vistas resuelven sus dependencias al crearse.
- Hibernate no toca el esquema (`ddl-auto=none`). La base la manda Flyway.

---

## 8. Dependencias entre módulos

```
person ──► user        (el alta de persona crea la cuenta)
checkin ─► person      (resolver la persona por documento)
attendance ► checkin   (la asistencia cuelga de una llegada)
payment ─► person, checkin
queue ───► (solo vistas)
report ──► (solo vistas)
history ─► (solo vistas)
```

`person → user` es la única dependencia entre services que cruza módulos, y existe porque
el alta encadena los dos procedimientos en una transacción. El resto se comunica por ids.

---

## 9. Convención de rutas

`PSR_Check-In_Endpoints.md` está alineado con el código: las rutas del documento son las
que responde el backend hoy. La regla que siguen ambos:

- **Filtro obligatorio de una sola clave → segmento de ruta.**
  `/api/sesiones/fecha/{fecha}`, `/api/movimientos/persona/{id}`,
  `/api/asistencias/historial/sesion/{id}`.
- **Filtro opcional o combinable → query param.**
  `?activo=`, `?diaSemana=`, `?desde=&hasta=`, `?sesionId=`.

Lo segundo evita una ruta por combinación de filtros; lo primero mantiene legible el caso
normal. Los endpoints que aún no tienen controller van marcados con ⏳ en el documento.

---

## 10. Lo que falta

- Módulo **auth** completo (login y recuperación de contraseña).
- Módulo **websocket**: `cola:actualizada` (tras check-in, confirmación o anulación) y
  `cola:configuracion-actualizada`. Los publica el service al cerrar su transacción, no un
  trigger ni un poller.
- Un bean de `PasswordEncoder`: sin él la aplicación no arranca, porque `UserService`
  lo inyecta.
