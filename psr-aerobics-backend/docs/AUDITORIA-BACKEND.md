# Auditoría del backend

Fecha: 2026-09-11.

No es una revisión de estilo: busqué **defectos funcionales por familias**, partiendo de la sospecha
de que el bug de los enums —que ya apareció tres veces— no fuera un caso aislado. Tenía razón: era
una familia. Este documento recorre esas familias, dice cuáles tienen víctimas reales y cuáles están
sanas.

Todo lo de abajo está verificado leyendo el código. Lo que no pude comprobar se dice.

---

## Resumen

| | |
|---|---|
| **Defectos que rompen hoy** | 6 — **todos arreglados en esta sesión** |
| **Defectos latentes** | 2 — no rompen aún, pero son la misma trampa que ya mordió dos veces |
| **Inconsistencias de diseño** | 2 — explican quejas del testeo sin ser bugs |
| **Familias verificadas y sanas** | 5 — documentadas para que nadie las revise otra vez |

La conclusión de fondo: **casi todos los fallos vienen de dos causas repetidas**, no de veinte errores
independientes. Entenderlas evita que vuelvan.

---

## Las dos causas raíz

### Causa A — Traducción inglés/español entre Java y Postgres

El proyecto tiene una convención sólida: constantes Java en inglés (`DOCUMENT`), valores de base en
español (`DOCUMENTO`), y un `AttributeConverter` que traduce. El problema es que **el converter solo
actúa en un sitio: entre una entidad y su columna.** No actúa:

- cuando MapStruct convierte un `String` a un enum → genera `valueOf()` y compara contra el nombre
  inglés,
- cuando Spring convierte un `@PathVariable` a un enum → mismo `valueOf()`,
- cuando JPA escribe un enum en una columna que Postgres declara como tipo enum → manda varchar.

**Cinco de los fallos reportados en tres rondas distintas son este mismo error en sitios distintos.**
Cada uno se reportó como bug independiente.

**La regla que evita que vuelva:** si un valor cruza la frontera entre Java y la base, o entre una
vista y un DTO, **tiene que estar tipado como el enum en los dos lados**. Un `String` en medio
significa que alguien va a hacer `valueOf()`.

### Causa B — El `@ExceptionHandler(Exception.class)` se traga cosas que no debería

El handler genérico convierte en `500 Error interno del servidor` cualquier excepción sin handler
propio. Eso incluía:

- rutas inexistentes (deberían ser 404),
- violaciones de CHECK del esquema (deberían ser 400),
- `ConflictException`, que existía sin handler,
- credenciales incorrectas, antes de que se le pusiera handler.

El efecto no es solo el status equivocado: **un 500 no lleva mensaje útil**, así que cada uno de estos
casos costó una sesión de depuración buscando en el servidor un fallo que estaba en la petición.

---

## 1. Defectos que rompían hoy — arreglados

### 1.1 El barrido de enums en vistas · **CRÍTICO**

Tres vistas declaraban `checkinMethod`/`documentType` como `String` y alimentaban DTO que los
declaran como enum. MapStruct generaba `valueOf("DOCUMENTO")` → 500.

| Vista | Rompía |
|---|---|
| `TodayQueueView` | check-in completo y la cola del día |
| `AttendanceHistoryView` | historial de asistencias |
| `PersonProfileView` | nada aún — coincidencia, ver 2.1 |

**Lo que lo hacía difícil de ver:** con `QR` funcionaba, porque ahí el nombre de la constante y el
valor de base coinciden. Solo fallaba con `DOCUMENTO` y `MANUAL`. Quien probara solo con QR concluía
que la cola funcionaba.

**Y el dato quedaba bien guardado.** El log lo muestra: *"Check-in registrado: ID 2, Persona 23"* y
justo después el 500. La escritura funcionaba; fallaba armar la respuesta. Eso significa que puede
haber check-ins reales en la base que el usuario vio como error.

### 1.2 Cualquier ruta inexistente respondía 500 en vez de 404

`NoHandlerFoundException` y `NoResourceFoundException` caían en el handler genérico. Afectaba a
**toda la API**. Es lo que hizo que `PATCH /api/metodos-pago/3/desactivar` —un endpoint que
simplemente no existía— pareciera un fallo del servidor.

### 1.3 `GET /api/tarifa` denegado a MIEMBRO

`PricingController` tenía `@PreAuthorize(STAFF)` a nivel de clase y el `GET` no lo sobrescribía.

**Descarto la teoría del timing** que planteaba el reporte (*"después de modificarla como ADMIN sí
cargó"*): la regla es estática y no depende de los datos. O hubo confusión de sesión o fue caché del
navegador. No son dos bugs.

### 1.4 Faltaban activar/desactivar método de pago

No existían. Se añadieron como `PATCH`, siguiendo el patrón de usuarios y horarios, **y no vía el PUT
existente**: ese exige `name` por ser `@NotBlank`, así que apagar un método obligaba a reenviar el
nombre que el cliente tuviera en pantalla, pisando cualquier renombrado concurrente.

### 1.5 `chk_sesion_horas` llegaba como 500 crudo

Ahora responde `400` con *"La hora de fin debe ser posterior a la hora de inicio"*. Se identifica por
**nombre de constraint**, no por el texto del mensaje de Postgres, que está traducido según el locale
del servidor y cambia entre versiones.

De paso quedó cubierto `chk_contacto_emergencia_no_vacio`, que tenía el mismo problema.

### 1.6 `?sort=` en los buscadores tumbaba el listado · **encontrado en esta auditoría**

Este no lo reportó nadie, y es el que más me llamó la atención.

`GET /api/personas?buscar=...` y `GET /api/usuarios?buscar=...` usan SQL nativo que trae su **propio**
`ORDER BY` —obligatorio, porque sin orden fijo la paginación repite y se salta filas—. Si el cliente
añade `?sort=firstName`, Spring Data concatena un segundo `ORDER BY`, el SQL deja de ser válido y el
endpoint responde 500.

El javadoc de los dos repositorios ya avisaba: *"este método NO admite un Pageable con Sort"*. Pero
**no había nada que lo impidiera**. Bastaba con que el frontend añadiera una ordenación a la tabla de
personas para tumbar la búsqueda.

Un comentario no es una defensa. Se añadió `SearchTerms.unsorted()` y los dos servicios lo aplican.

---

## 2. Latentes — no rompen hoy, pero son la trampa que ya mordió dos veces

### 2.1 `PersonProfileView.documentType` funcionaba por coincidencia

Era `String` alimentando un `DocumentType`. No fallaba **solo porque `CC`, `TI`, `CE` y `PA` se
llaman igual en Java y en la base**. El día que se añadiera un tipo con nombre distinto, se caía como
el check-in. Ya está tipado.

### 2.2 Tres entidades con columna enum de Postgres y sin `@DynamicUpdate`

`Checkin.checkinMethod`, `Role.name` y `ChangeHistory.affectedTable` usan converter —que bindea
varchar— sobre columnas que Postgres declara como tipo enum. **Hoy no rompen porque ninguna se
escribe desde JPA**: las crean procedimientos, triggers o el seed de `V1`.

Pero esa es exactamente la combinación que ya produjo dos 500 en producción (`rol_nombre_enum` en
profesores, `tipo_documento_enum` al desactivar personas). El día que alguien añada un
`checkinRepository.save(...)`, vuelve.

`Person` y `AccountMovement` sí llevan `@DynamicUpdate` y están protegidas.

**No lo cambié**: añadir `@DynamicUpdate` a tres entidades que nadie escribe es tocar código que
funciona, a 48 h de la entrega. Queda anotado como lo primero a hacer si alguien va a escribir esas
tablas desde Java.

---

## 3. Inconsistencias que explican quejas del testeo

### 3.1 La auditoría cubre 4 tablas en la práctica, no 6

`tabla_auditable_enum` declara seis, pero:

| Tabla | ¿Se audita de verdad? |
|---|---|
| `TARIFA` | sí — trigger al cambiar el monto |
| `CONFIGURACION_COLA` | sí — trigger por cada campo |
| `MOVIMIENTO_CUENTA` | sí — al corregir un pago |
| `SESION` | sí — al cancelar y al cierre automático |
| `PERSONA` | **parcial** — solo reactivar y eliminar. Editar nombre, teléfono o documento **no deja rastro** |
| `ASISTENCIA` | **nunca** — el trigger existe y funciona, pero **ningún endpoint corrige `monto_cobrado`**, así que no hay forma de dispararlo |

Esto explica directamente el *"auditoría al parecer no funciona"*: la pantalla se ve vacía o casi,
no porque falle, sino porque **hay muy poco que auditar**. Los roles y los horarios, como ya sabías,
ni siquiera tienen valor en el enum.

### 3.2 `campo` es el nombre de la columna, no lenguaje humano

El endpoint **sí manda** `field`, `oldValue` y `newValue` con datos — confirmado contra los triggers
y procedimientos. Lo que pasa es que `field` vale `activo`, `cancelada`, `monto`,
`personas_por_turno`. Exacto, pero ilegible para quien mira la pantalla.

Ya está decidido que la traducción la hace frontend.

---

## 4. Familias que revisé y están sanas

Las dejo escritas para que nadie las vuelva a mirar.

| Familia | Resultado |
|---|---|
| **Contexto de auditoría** (`app.usuario_id`) | ✅ Los cuatro caminos que escriben tablas auditadas llaman a `setCurrentUser` antes: corregir movimiento, entregar vuelto, tarifa y config de cola. Es el punto más frágil del proyecto y está bien puesto. |
| **`@Transactional` en escrituras** | ✅ Ningún método de servicio que escribe se quedó sin transacción. |
| **Otros `@OneToOne` inversos** (el N+1) | ✅ Solo había uno, `Person.emergencyContact`, y ya se corrigió al pasar los contactos a 1:N. `Attendance.checkin` es el lado **dueño** (`@JoinColumn`), así que el LAZY sí funciona. |
| **Controllers sin autorización** | ✅ Solo `AuthController`, y es correcto: sus rutas públicas están listadas una a una en `SecurityConfig` y el resto cae en `anyRequest().authenticated()`. |
| **`EmailException` escapando a un controller** | ✅ No puede: el único camino pasa por `AuthEmailListener`, que la captura. |

---

## 5. Lo que esta auditoría **no** cubre

Honestidad sobre el alcance:

- **Nada está ejecutado.** Todo es análisis estático. La base sigue sin formatearse, así que ni el
  SQL se ha aplicado ni ningún endpoint se ha llamado. Con `ddl-auto=none`, una columna mal escrita
  en una entidad no falla al arrancar sino al consultar — este tipo de fallo **no lo puede encontrar
  una lectura**, solo una ejecución.
- **Rendimiento: sin medir.** La queja de lentitud sigue sin diagnosticar. Lo único que puedo afirmar
  es que el N+1 de contactos ya no existe y que las entidades de vista son planas, sin relaciones, así
  que no generan N+1 por sí solas. Medir requiere datos y una ejecución real.
- **Concurrencia.** No revisé condiciones de carrera. Con un gimnasio de este tamaño no es la
  prioridad, pero el tope de cinco contactos, por ejemplo, se comprueba con un `count` y luego un
  `insert`: dos peticiones simultáneas podrían colarse. El trigger de base es lo que lo garantiza de
  verdad, y ese sí es atómico.
- **No hay tests.** `src/test` está vacío. Todo lo verificado aquí lo está por lectura, no por
  ejecución repetible.

---

## 6. Recomendación

Con 48 h por delante, el orden que maximiza lo que queda en pie:

1. **Formatear la base y arrancar.** Es lo único que puede encontrar la clase de fallo que una
   lectura no ve, y no se ha hecho desde el 9 de septiembre. Cada día que pasa acumula más código sin
   verificar.
2. **Probar el check-in con `DOCUMENTO` y `MANUAL`, no solo con QR.** El fallo de los enums
   sobrevivió tres rondas porque QR funcionaba de casualidad.
3. **Dejar la auditoría como está.** Ya está decidido y es la decisión correcta: cubre 4 tablas de 6
   y ampliarla toca esquema.
4. **No tocar las tres entidades del punto 2.2** salvo que haya que escribirlas desde Java.
