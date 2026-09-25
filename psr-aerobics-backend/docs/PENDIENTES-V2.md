# Pendientes — v2

Lo que se decidió **no** hacer en la v1, con el motivo y el estado exacto del
código, para que retomarlo no obligue a redescubrirlo.

No confundir con `PENDIENTES.md` (lo aplazado tras el MVP, rondas anteriores) ni
con `pendiente-implementar-sistema.md` (lo que falta para que la política de datos
sea cierta).

---

## 1. Consultar saldo o perfil por número de documento — media implementación ya hecha

**Hoy hay que hacer dos llamadas** para saber el saldo de alguien a partir de su
documento:

```
GET /api/personas/buscar?documento=4444   → devuelve el id
GET /api/personas/{id}/saldo              → devuelve el saldo
```

**Lo que ya está en el código.** `PersonService.getProfileByDocumentNumber(...)`
existe, está implementado y funciona: resuelve el perfil completo —datos, saldo,
días de crédito y resumen de asistencias— a partir del número de documento, en una
sola consulta contra `vista_perfil_persona`. Se apoya en
`PersonProfileViewRepository.findByDocumentNumber(...)`, que también existe.

**Lo que falta: la ruta.** Ningún controller lo expone. Es un método de servicio
sin `@GetMapping`, así que desde la API no hay forma de llamarlo.

**Lo que habría que decidir al retomarlo:**

- La forma de la ruta. `GET /api/personas/perfil?documento=4444` encaja con el
  `GET /api/personas/buscar?documento=` que ya existe; un `/saldo?documento=`
  aparte obligaría a un repositorio nuevo, porque `PersonBalanceViewRepository`
  solo tiene `findByPersonId`.
- El permiso. `/buscar` es `PERSON_LOOKUP` (incluye `PSR_TERMINAL`) y devuelve a
  propósito **datos mínimos, sin dinero** — ver el javadoc de
  `TerminalPersonLookupResponse`. Un perfil por documento sí lleva saldo, así que
  no debería heredar ese permiso sin pensarlo: la terminal del lobby no tiene por
  qué ver cuánto debe nadie.
- Si hace falta de verdad. Las dos llamadas actuales funcionan y el frontend ya
  las encadena. Esto es comodidad, no una carencia.

**Por qué no entró en v1.** No lo pidió ninguna pantalla; salió al responder una
consulta sobre cómo mirar el saldo por documento y se anotó para no perderlo.

---

## 2. Verificación pendiente de la ronda 18-sep: el SQL no se ejecutó

Los cambios de esa ronda en `R__VIEWS.sql` (columna `reembolsable` en
`vista_saldo_persona` y en `vista_fila_hoy`) y en `R__STORED_PROCEDURES.sql`
(`sp_registrar_reembolso` hereda el `llegada_id` del abono) **se escribieron sin
poder ejecutarlos contra una base real**. El Java compila y está verificado; el SQL
no.

**Qué comprobar en el primer arranque:**

1. Que Flyway reaplica los dos repetibles sin error. `CREATE OR REPLACE VIEW` solo
   admite columnas **añadidas al final** — por eso `reembolsable` va la última en
   las dos vistas. Si alguien la mueve de sitio, falla contra cualquier base que ya
   tenga la versión anterior.
2. Que `reembolsable` cuadra. La consulta que lo verifica, calculando lo mismo
   desde las tablas base, está en el historial de esta ronda: si el cálculo directo
   y la vista dan lo mismo para varias personas con abonos, reembolsos parciales y
   asistencias, la vista es correcta.
3. **Efecto secundario conocido del cambio en el reembolso.** Ahora que el
   reembolso cuelga de una llegada, resta del recaudo de esa sesión en
   `vista_recaudo_por_sesion` y `vista_recaudo_diario`. Es más correcto que antes
   —devolver dinero baja la caja—, pero conviene mirarlo si alguna pantalla daba
   por hecho que el recaudo solo sube.
4. **De la ronda del 21-sep (la del deploy), lo mas sensible:** la confirmacion
   automatica de asistencia cuando el credito cubre la sesion. Se apoya en que la
   columna `cubierto_por_saldo` de `vista_fila_hoy` es cierta **solo** si no hay
   asistencia y el monto a pagar es cero — de ahi sale la idempotencia. **Si esa
   columna se calculara mal, se descontarian sesiones de mas**, asi que es lo primero
   que hay que ver con datos reales: persona con credito 5 marca llegada y su perfil
   debe decir 4, no 3.

   Tambien de esa ronda: `SessionStatusScheduler`, que publica `SESSION_CHANGED`
   cuando una sesion empieza o acaba por el reloj. Es el unico scheduler que depende
   de que su `@Transactional` funcione — sin transaccion, el evento se descarta en
   silencio y no hay error que lo delate. Se comprueba mirando el log alrededor de la
   hora de inicio de una clase.
5. **De la ronda 2 del 20/21-sep:** `fn_monto_a_pagar` (nueva en `R__FUNCTIONS.sql`) y
   dos columnas mas en `vista_fila_hoy` (`monto_a_pagar`, `cubierto_por_saldo`). La
   tabla de seis casos de cobro del punto 2 de esa ronda esta respondida **por
   razonamiento sobre el modelo, sin ejecutar**: es lo primero que conviene comprobar
   con datos reales, porque es la cifra del cobro.
5. **De la ronda del 20-sep:** las tres funciones nuevas `fn_vuelto_pendiente`,
   `fn_estado_cuenta` y `fn_estado_sesion`, mas la regla de "fuera de horario" dentro
   de `fn_obtener_o_crear_sesion_del_momento` — que ademas **reemplaza** la version
   sin parametro con un `DROP FUNCTION` previo. Si algo del arranque falla, empezar
   por ahi.

   El valor `PROPIO` de `metodo_checkin_enum` llego a vivir en una migracion
   `V2__CHECKIN_PROPIO.sql`; el **21-sep se plegó dentro de `V1`** y la V2 se borro,
   porque la base se iba a recrear de cero y una migracion incremental para un
   esquema que nunca llego a existir solo era ruido. **El proyecto vuelve a tener una
   sola migracion versionada.**
6. **De la ronda del 19-sep, también sin ejecutar:** `fn_saldo_reembolsable` (nueva
   en `R__FUNCTIONS.sql`) y el `HistoricalDataSeeder`, que escribe con INSERT
   directos. El seed es, de hecho, la forma más rápida de verificar la función:
   arranca con `SEED_HISTORICAL_ENABLED=true` y comprueba que la persona `70041`
   (abonó 15.000, consumió 10.000) devuelve `refundable` = **5.000** y no 15.000.

---

## 3. Método de pago propio para la entrega del vuelto

`POST /api/movimientos/{id}/vuelto` no crea un movimiento: marca
`vuelto_entregado = true` sobre el abono original. El método de pago es por tanto
el de ese abono, siempre.

**Lo que no se puede hoy:** cobrar por transferencia y entregar el vuelto en
efectivo. Registrarlo exigiría una columna nueva (`metodo_pago_vuelto_id`) o un
movimiento aparte.

El coste depende de cuando se pida. **Mientras la base se siga pudiendo recrear de
cero, es barato**: se añade la columna a `V1` y ya esta. En cuanto haya datos reales
que no se puedan perder, pasa a exigir una migracion incremental y a ser mucho mas
caro. Si el caso va a ocurrir, conviene decidirlo antes de ese punto.

**Por qué no entró:** el frontend lo preguntó sin saber si el caso ocurre de
verdad. Cambiar el esquema por un caso hipotético es caro; si aparece en
operación real, se retoma con el dato en la mano.

---

## 4. La causa raíz de Brevo sigue abierta

El log ya dice **qué** responde Brevo (código HTTP y cuerpo), que es lo que antes
faltaba. Lo que no cierra el backend es **por qué**: hay que mirar en el panel los
créditos del día, el estado de la API key y el remitente verificado
(`app.email.from-address`). Ver B1 en `pendientes-back.md`.
