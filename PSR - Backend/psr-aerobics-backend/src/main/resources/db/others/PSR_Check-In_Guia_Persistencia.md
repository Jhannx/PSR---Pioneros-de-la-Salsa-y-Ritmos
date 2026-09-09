# Guía de Desarrollo — Persistencia (PSR Check-In)

Puente entre el esquema de base de datos y el código Java. Ábrelo
mientras escribes cada `Repository`/`Service`: te dice si existe un
procedimiento que debas invocar, si es una vista, o si es JPA normal.

**Sobre la confianza en este documento:** el SQL está probado contra
Postgres 16 real — cada procedimiento, trigger y función, con sus casos
límite. El código Java de esta guía es patrón estándar de Spring JDBC
**no ejecutado aquí** (no hay runtime de Spring en el entorno donde se
construyó el esquema). Tú lo compilas y confirmas; el punto a vigilar
está marcado explícitamente abajo (binding de enums de Postgres).

---

## 1. Patrones generales

### A. Procedimiento con parámetro `OUT`

Usa `SimpleJdbcCall`, no `@Procedure` de Spring Data JPA — esa anotación
está pensada sobre todo para funciones y da problemas con procedimientos
Postgres que usan `CALL` + `OUT`.

```java
@Repository
public class LlegadaRepository {

    private final SimpleJdbcCall registrarLlegadaCall;

    public LlegadaRepository(JdbcTemplate jdbcTemplate) {
        this.registrarLlegadaCall = new SimpleJdbcCall(jdbcTemplate)
            .withProcedureName("sp_registrar_llegada")
            .withoutProcedureColumnMetaDataAccess()
            .withNamedBinding()
            .declareParameters(
                new SqlParameter("p_persona_id", Types.INTEGER),
                new SqlParameter("p_metodo_checkin", Types.OTHER),
                new SqlOutParameter("p_llegada_id", Types.INTEGER)
            );
    }

    public Integer registrarLlegada(Integer personaId, String metodoCheckin) {
        var params = new MapSqlParameterSource()
            .addValue("p_persona_id", personaId)
            .addValue("p_metodo_checkin", metodoCheckin, Types.OTHER);
        var result = registrarLlegadaCall.execute(params);
        return (Integer) result.get("p_llegada_id");
    }
}
```

⚠️ **Punto a confirmar tú:** `p_metodo_checkin` es un enum de Postgres
(`metodo_checkin_enum`), no un `varchar`. `Types.OTHER` suele funcionar
con el driver de Postgres; si da error de cast, envuélvelo en un
`PGobject`:

```java
PGobject metodo = new PGobject();
metodo.setType("metodo_checkin_enum");
metodo.setValue("DOCUMENTO");
```

Aplican a este patrón: `sp_registrar_persona`,
`sp_registrar_usuario_para_persona`, `sp_registrar_llegada`,
`sp_crear_sesion_manual`, `sp_solicitar_recuperacion`.

### B. Procedimiento sin `OUT`

`JdbcTemplate.update()` directo con sintaxis `CALL`:

```java
jdbcTemplate.update("CALL sp_anular_asistencia(?)", llegadaId);
jdbcTemplate.update("CALL sp_eliminar_persona(?, ?, ?)", personaId, adminId, true);
jdbcTemplate.update("CALL sp_reactivar_persona(?, ?)", personaId, adminId);
```

### C. `set_config` de auditoría — dónde va exactamente

Debe ejecutarse en la **misma transacción/conexión** que la operación que
audita. Primera línea de cada método `@Transactional` que toque
`asistencia`, `movimiento_cuenta`, `tarifa`, `configuracion_cola`, o que
llame a `sp_reactivar_persona`/`sp_cancelar_sesion`:

> ⚠️ `sp_eliminar_persona` es la excepción: configura `app.usuario_id`
> **internamente** con `ejecutado_por_usuario_id`, así que la app no
> necesita hacer `set_config` antes de llamarlo.

```java
@Transactional
public void confirmarAsistencia(Integer llegadaId, Integer usuarioId) {
    jdbcTemplate.update("SELECT set_config('app.usuario_id', ?, true)",
        usuarioId.toString());
    jdbcTemplate.update("CALL sp_registrar_asistencia(?, ?)", llegadaId, usuarioId);
}
```

Si se te olvida, la operación **falla con un mensaje claro** (no en
silencio) — lo notarás en la primera prueba.

### D. Vistas — entidades de solo lectura

```java
@Entity
@Immutable // org.hibernate.annotations
@Table(name = "vista_fila_hoy")
public class FilaHoyView {
    @Id
    @Column(name = "llegada_id")
    private Integer llegadaId;
    // ... resto de columnas, mismo nombre que en la vista
}
```

El repositorio **no debe extender `JpaRepository`** (trae `save`/`delete`,
que no deberían ni estar disponibles) — extiende
`Repository<FilaHoyView, Integer>` con solo los métodos de consulta que
necesites.

### E. Capturar `SQLSTATE 75001` — persona desactivada

`fn_validar_persona_activa` lanza este código cuando la persona existe
pero está desactivada, distinto del error genérico de "no existe".
Captúralo por código, nunca por texto del mensaje:

```java
import org.postgresql.util.PSQLException;

try {
    jdbcTemplate.update("CALL sp_registrar_llegada(?, ?, NULL)", personaId, metodoCheckin);
} catch (DataAccessException e) {
    Throwable cause = e.getRootCause();
    if (cause instanceof PSQLException psql) {
        if ("75001".equals(psql.getSQLState())) {
            throw new PersonaDesactivadaException(personaId); // 409 + "¿reactivar?"
        }
        if ("75002".equals(psql.getSQLState())) {
            throw new SinClaseVigenteException(); // 409 + "no hay clase ahora"
        }
    }
    throw e;
}
```

### F. `SQLSTATE 75002` — no hay clase en este momento

Mismo mecanismo que E (ver el bloque de arriba, cubre los dos). Lo lanza
`fn_obtener_o_crear_sesion_del_momento` cuando ningún horario está
vigente. Son **dos mensajes distintos para el usuario final** — no los
mezcles en un `catch` genérico.

Ambos pertenecen al `GlobalExceptionHandler` de `shared/exception/`, no
repetidos en cada Service.

### G. De dónde sale el `usuario_id` de un procedimiento

Varios procedimientos reciben el id de quien ejecuta la acción
(`p_usuario_id`, `p_ejecutado_por_usuario_id`, `marcado_por`). **Ese
valor sale siempre del JWT, nunca del body del request.**

```java
// En el Controller
@PutMapping("/api/llegadas/{id}/asistencia")
public AttendanceResponse confirmar(@PathVariable Integer id,
                                    @AuthenticationPrincipal UserPrincipal principal) {
    return attendanceService.confirm(id, principal.getUserId());
}
```

Si el id viniera en el body, cualquier cliente podría registrar
asistencias, pagos o eliminaciones **a nombre de otro usuario** — y la
auditoría (`historial_cambio`, `asistencia.marcado_por`) quedaría
apuntando a quien el atacante quisiera. No es un detalle de estilo.

Procedimientos afectados: `sp_registrar_asistencia`,
`sp_registrar_movimiento`, `sp_registrar_reembolso`,
`sp_reactivar_persona`, `sp_cancelar_sesion`, `sp_eliminar_persona`.

Consecuencia para los DTOs: **ningún `…Request` de este proyecto lleva
un campo `userId` / `markedByUserId` / `executedByUserId`.** Si lo tiene,
sobra.

### H. Un `CALL` no devuelve el `Response` — hay que releer

Los procedimientos devuelven, como mucho, un id por `OUT`. Todo lo demás
que el `…Response` necesita (nombres, montos calculados, posición en la
cola) hay que **leerlo después del `CALL`, dentro de la misma
transacción**.

```java
@Override
@Transactional
public AttendanceResponse confirm(Integer checkinId, Integer markedByUserId) {
    auditContextRepository.setCurrentUser(markedByUserId);      // patrón C
    attendanceRepository.registrarAsistencia(checkinId, markedByUserId);  // patrón B

    // El SP no devuelve el monto: lo calculó fn_tarifa_vigente() adentro.
    // Hay que releer la fila recién creada para poder responder.
    Attendance saved = attendanceRepository.findByCheckinId(checkinId)
            .orElseThrow(() -> new IllegalStateException(
                "La asistencia no quedó registrada para la llegada " + checkinId));

    return attendanceMapper.toResponse(saved);
}
```

Casos donde aplica, y qué hay que releer:

| Procedimiento | El `OUT` da | Para el `Response` falta leer |
|---|---|---|
| `sp_registrar_asistencia` | *(nada)* | `asistencia` — sobre todo `monto_cobrado`, que calcula la BD |
| `sp_registrar_llegada` | `llegada_id` | `vista_fila_hoy` — nombre, sesión y `posicion` |
| `sp_registrar_persona` | `persona_id` | `persona` — `fecha_registro`, `activo` |
| `sp_registrar_movimiento` | *(nada)* | `movimiento_cuenta` — id, `fecha_hora`, nombre del método de pago |

Este es el motivo por el que un `…Response` casi nunca se puede
construir solo con lo que el cliente envió: la mitad de sus campos los
pone Postgres.

---

## 2. Referencia rápida — las 16 firmas exactas

```sql
sp_registrar_persona(tipo_documento, numero_documento, nombre, apellidos, telefono, fuente_registro, OUT persona_id)
sp_registrar_usuario_para_persona(persona_id, password_hash, OUT usuario_id)
sp_asignar_rol(usuario_id, rol_nombre)
sp_quitar_rol(usuario_id, rol_nombre)
sp_reactivar_persona(persona_id, ejecutado_por_usuario_id)
sp_eliminar_persona(persona_id, ejecutado_por_usuario_id, force)

sp_registrar_llegada(persona_id, metodo_checkin, OUT llegada_id)
sp_registrar_asistencia(llegada_id, usuario_id)
sp_anular_asistencia(llegada_id)

sp_crear_sesion_manual(fecha, hora_inicio, hora_fin, profesor_usuario_id, horario_id, OUT sesion_id)
sp_cancelar_sesion(sesion_id, ejecutado_por_usuario_id)

sp_registrar_movimiento(persona_id, llegada_id, monto_recibido, monto_abonado, usuario_id, metodo_pago_id, nota DEFAULT NULL)
sp_registrar_reembolso(movimiento_original_id, monto, usuario_id, metodo_pago_id, nota DEFAULT NULL)
sp_entregar_vuelto(movimiento_id)

sp_solicitar_recuperacion(usuario_id, OUT token)
sp_restablecer_password(token, nueva_password_hash)
```

---

## 3. Clase por clase

### `PersonaRepository` / `PersonaService`

| Acción | Mecanismo |
|---|---|
| Buscar por nombre/documento, listar | JPA — método derivado o `Specification`. Sin filtro de `eliminado` — no existe la columna, una persona eliminada ya no es una fila. |
| Detalle por id | JPA — `findById`. |
| **Registrar** (admin o autoregistro) | **2 SPs en un solo `@Transactional`**: `sp_registrar_persona` (patrón A, ahora recibe `tipo_documento` como enum — ojo con el binding, ver patrón A) + `sp_registrar_usuario_para_persona` (patrón A, asigna rol `MIEMBRO` solo). El hash se calcula en Java antes de la segunda llamada. Si hay contacto de emergencia, un tercer paso: `save()` plano. |
| Editar datos básicos | JPA — `save()`. |
| Desactivar (`activo = false`) | JPA — `save()`. `trg_sincronizar_usuario_con_persona` propaga al `usuario` solo. |
| **Reactivar** | **SP** — `sp_reactivar_persona` (patrón B + C). El mismo trigger reactiva el `usuario`. |
| **Eliminar definitivo** | **SP** — `sp_eliminar_persona` (patrón B; el patrón C ahora es interno al procedimiento, no lo hace la app). Irreversible: borra `persona`/`usuario` de verdad, reasignando su huella al sentinel. |
| Ver perfil consolidado | **Vista** — `vista_perfil_persona` (patrón D). |

### `UsuarioRolRepository` / `UserService`

| Acción | Mecanismo |
|---|---|
| Listar roles de un usuario | JPA — JOIN a `usuario_rol`. |
| **Asignar un rol** | **SP** — `sp_asignar_rol` (patrón B). Idempotente. |
| **Quitar un rol** | **SP** — `sp_quitar_rol` (patrón B). Falla si dejaría al usuario sin roles. |
| Listar profesores asignables | JPA — `usuario` JOIN `usuario_rol` WHERE rol = `PROFESOR`. |

⚠️ **Spring Security:** `usuario` **ya no tiene `rol_id`**. El
`UserDetailsService` debe cargar la lista completa de roles desde
`usuario_rol` como authorities — si copias un ejemplo típico de "un rol
por usuario", no va a funcionar aquí.

### `ContactoEmergenciaRepository`

| Acción | Mecanismo |
|---|---|
| Ver / crear / editar | JPA plano. 1:1 opcional con `persona` — upsert manual: buscar por `persona_id`, si no existe, crear. |

### `HorarioRepository` / `ScheduleService`

| Acción | Mecanismo |
|---|---|
| Listar/crear/editar horarios | JPA plano. Sin unicidad por día — `save()` nunca falla por duplicado. |
| Desactivar un horario | JPA — `activo = false` (nunca borrado físico: hay sesiones ligadas). |
| Declarar excepción de horario | JPA plano sobre `horario_excepcion`. La `UNIQUE (horario_id, fecha)` evita declararla dos veces. |

### `SesionRepository` / `ScheduleService`

| Acción | Mecanismo |
|---|---|
| Listar/buscar sesiones | JPA — método derivado o `Specification`. |
| Resumen de sesión(es) | **Vista** — `vista_resumen_sesiones` (patrón D). |
| **Crear sesión manual** | **SP** — `sp_crear_sesion_manual` (patrón A). Nunca se bloquea por rango horario. Recibe `profesor_usuario_id`; falla si ese usuario no tiene rol `PROFESOR`. |
| **Cancelar sesión** | **SP** — `sp_cancelar_sesion` (patrón B + C). |

**No existe** un `save()` para el flujo de check-in normal — esa creación
siempre pasa por `sp_registrar_llegada` →
`fn_obtener_o_crear_sesion_del_momento()`.

**Para depurar la resolución de sesión** (cuando algo no cuadre en
producción), esa función se apoya en dos más chicas que puedes consultar
sueltas desde `psql`, sin efectos secundarios — ninguna crea nada:

```sql
SELECT fn_sesion_abierta_ahora();          -- ¿hay sesión abierta ahora? (id o NULL)
SELECT * FROM fn_horario_vigente_ahora();  -- ¿qué horario aplica ahora? (o NULL)
```

### `LlegadaRepository` / `AttendanceService`

| Acción | Mecanismo |
|---|---|
| Check-in (QR/documento/manual) | **SP** — `sp_registrar_llegada` (patrón A). Puede lanzar `75001` **y** `75002` — captúralos por separado (patrones E y F). |

### `AsistenciaRepository` / `AttendanceService`

| Método del service | Qué es en la BD | Mecanismo |
|---|---|---|
| `confirm(checkinId, 🔐)` | **Crea** la fila de `asistencia` de esa llegada | **SP** — `sp_registrar_asistencia` (patrón B + C + **H**) |
| `cancel(checkinId)` | **Borra** esa fila. La `llegada` no se toca | **SP** — `sp_anular_asistencia` (patrón B) |
| `findByCheckinId(checkinId)` | Lectura puntual | JPA — `findByLlegadaId` |
| `findBySession(sessionId)` | Asistencias de una sesión | JPA — join con `llegada` |
| `findByPerson(personId, pageable)` | Asistencias de una persona | JPA |
| `isConfirmed(checkinId)` | ¿Existe la fila? | JPA — `existsByLlegadaId` |
| `getHistoryByPerson` / `getHistoryBySession` / `getHistoryBetween` | **No es la tabla** | **Vista** — `vista_historial_asistencias` (patrón D) |
| `getBalanceByPerson(personId)` | **No es la tabla** | **Vista** — `vista_saldo_por_asistencia` (patrón D) |

Tres cosas que no son obvias leyendo solo la interfaz:

1. **`confirm` es un create, no un update.** "Confirmar" suena a
   modificar algo que ya existe, pero la fila de `asistencia` no existe
   hasta ese momento — antes solo hay `llegada`. Y `cancel` es un
   `DELETE` de esa fila, no un flag: no hay columna "anulada".
2. **`checkinId` es `llegada.id`, no `asistencia.id`.** Toda la API de
   asistencia se direcciona por la llegada
   (`/api/llegadas/{id}/asistencia`), porque es lo que el admin tiene en
   pantalla cuando decide confirmar.
3. **Los últimos cuatro métodos no pertenecen a la entidad `Attendance`.**
   Devuelven `…View` de vistas SQL, con entidades `@Immutable` propias.
   Mapearlos desde `Attendance` obliga a reconstruir a mano joins y
   cálculos que la vista ya resuelve.

No existe un `save()` normal para crear una asistencia — la única puerta
es el procedimiento, porque cobra la tarifa vigente en el mismo
instante. No lo repliques con JPA aunque parezca un `INSERT` simple.

⚠️ `confirm` **no recibe monto** y **no recibe un `AttendanceRequest`**:
el monto lo calcula `fn_tarifa_vigente()` dentro del SP y el usuario sale
del JWT. Ver patrones G y H.

### `MetodoPagoRepository`

| Acción | Mecanismo |
|---|---|
| Listar métodos activos (para un select) | JPA — `findByActivoTrue()`. |
| Crear/editar un método | JPA plano — `save()`. |

**Cambio de tipo importante:** `metodo_pago` **ya no es un enum** — es
una tabla. En los DTOs, el campo pasa a ser `Long metodoPagoId` (o una
relación `@ManyToOne` a la entidad `MetodoPago`). Cualquier `switch`
sobre un enum viejo hay que reescribirlo.

### `MovimientoRepository` / `PaymentService`

| Acción | Mecanismo |
|---|---|
| Registrar un abono | **SP** — `sp_registrar_movimiento` (patrón B). Valida persona activa → puede lanzar `75001`. |
| Corregir monto/método mal digitado | JPA — `save()` + `set_config` antes (patrón C). Se audita solo. |
| **Reembolsar** | **SP** — `sp_registrar_reembolso` (patrón B). No lo repliques con JPA: valida el tope contra el monto original. |
| Entregar vuelto | **SP** — `sp_entregar_vuelto` (patrón B). |
| Vueltos pendientes | **Vista** — `vista_vueltos_pendientes`. |
| Recaudo diario | **Vista** — `vista_recaudo_diario`. |
| Saldo de una persona | **Vista** — `vista_saldo_persona`. |
| Cobertura sesión por sesión | **Vista** — `vista_saldo_por_asistencia`. |

### `ColaRepository` / `QueueService`

| Acción | Mecanismo |
|---|---|
| Cola completa (admin) | **Vista** — `vista_fila_hoy`. Posición calculada **por sesión**. |
| Cola de sala de espera, pendientes (TV) | **Vista** — `vista_fila_espera`. Sin datos de dinero. |
| Cola de sala de espera, ya pagaron (TV) | **Vista** — `vista_fila_prepagada`. Sin datos de dinero. Solo cuenta pago hecho antes de la llegada — no confundir con "tiene saldo suficiente ahora". |
| Leer/actualizar configuración | JPA plano — `configuracion_cola`, fila única id=1. Expone `personas_por_turno`, `minutos_margen_ingreso` y `minutos_visibilidad_pagos`. El `UPDATE` necesita `set_config` (se audita). |

### `TarifaRepository`

| Acción | Mecanismo |
|---|---|
| Leer tarifa vigente | JPA — `findById(1)`. |
| Cambiar tarifa | JPA — `save()` + `set_config` (patrón C). |

### `UsuarioRepository` / `AuthService`

| Acción | Mecanismo |
|---|---|
| Login | JPA — `findByNumeroDocumentoOrCorreo` + `PasswordEncoder.matches()`. |
| Solicitar recuperación | **SP** — `sp_solicitar_recuperacion` (patrón A). |
| Restablecer contraseña | **SP** — `sp_restablecer_password` (patrón B). Recibe el **hash**, nunca la contraseña en claro. |

No existe un `save()` para crear un `MIEMBRO` por separado — siempre
nace junto con la persona (ver `PersonaService.registrar()`).

---

## 4. Checklist antes de escribir un método nuevo

1. ¿Existe ya como procedimiento (sección 2)? → Úsalo, no lo
   reconstruyas con JPA aunque parezca simple.
2. ¿Es una lectura que ya existe como vista? → Entidad `@Immutable`,
   nunca reconstruyas el `JOIN` a mano.
3. ¿Toca `asistencia`, `movimiento_cuenta`, `tarifa`,
   `configuracion_cola`, o llama a `sp_reactivar_persona` /
   `sp_cancelar_sesion`? → `set_config` primero, siempre.
   (`sp_eliminar_persona` es la excepción — lo configura internamente.)
4. ¿Llama a `sp_registrar_llegada` o `sp_registrar_movimiento`? →
   captura `75001` y `75002` por separado (patrones E y F).
5. ¿Toca método de pago? → ya no es enum, es FK — revisa que el
   DTO/entidad esté actualizado.
6. ¿Toca roles? → `usuario` no tiene `rol_id`; los roles son una
   colección desde `usuario_rol`.
7. ¿Toca montos de pago? → la columna se llama `monto_recibido`, no
   `efectivo_recibido`.
8. ¿El método necesita saber **quién** lo ejecuta? → ese id sale del
   JWT (patrón G), nunca de un campo del `…Request`.
9. ¿Devuelve un `…Response` después de un `CALL`? → el SP no te da los
   campos calculados; hay que releer (patrón H).
10. ¿Estás creando un DTO nuevo? → revisa la regla de solapamiento en
    `PSR_Check-In_Endpoints.md` (≥80 % reutilizar, <50 % dedicado) antes
    de duplicar. Y nunca reutilices un `Response` como `Request`.
11. Si nada de lo anterior aplica → JPA plano (`save()`/`findBy...`).
