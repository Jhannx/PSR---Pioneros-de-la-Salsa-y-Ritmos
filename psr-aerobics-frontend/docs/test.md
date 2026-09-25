# Notas de prueba

Cosas que verificar en el test completo (el del día del deploy de prueba). No son
pedidos al backend ni errores confirmados: son puntos abiertos que salieron
probando y que conviene cerrar con datos reales.

---

## Reportes y gráficos según el estado de la sesión

Lo esperado, dicho con las palabras del 19/09:

- **Sesión que se da y termina** → sus estadísticas **deben permanecer** en los
  reportes y gráficos.
- **Sesión cancelada** → está bien que sus estadísticas se eliminen.

**Estado de la prueba:**

- Caso cancelada: **observado** — al cancelar una sesión, sus datos desaparecen de
  los gráficos. Es lo esperado, no es un error.
- Caso sesión terminada: **sin probar todavía.** Falta comprobar que una sesión que
  sí se dio (con asistencias y cobros) conserva sus estadísticas después de
  cerrarse. Es el caso que importa: si ahí se pierden, sí sería un error.

**Detalle a mirar cuando se pruebe:** una sesión cancelada que ya tenía plata cobrada
hace que ese recaudo deje de figurar en los reportes. Para v1 se acepta; si más
adelante no conviene, se revisa.

Con el seed (más abajo) esta prueba es inmediata: hay 5 sesiones pasadas cerradas y
una cancelada ya cargadas.

---

## Datos de prueba (seed del back, ronda 19-09)

Apagado por defecto. Para cargarlo el backend necesita `SEED_ENABLED=true` (cuentas)
y además `SEED_HISTORICAL_ENABLED=true`. Depende de que ya existan un ADMIN y un
PROFESOR; si no, avisa en el log y no escribe nada.

Carga 45 personas (documentos `70001`–`70045`, **contraseña = el documento**), 5 días
hábiles pasados con 30 asistencias por sesión, una sesión abierta hoy, una sesión
cancelada con 3 check-ins colgando, una excepción de horario, y 3 métodos de pago:
EFECTIVO y TRANSFERENCIA activos, **BONO REGALO desactivado**.

Casos límite, cada uno en su propia persona:

| Documento | Qué debe verse |
|---|---|
| `70041` | El del reporte: abonó 15.000, consumió 10.000, saldo 5.000 → el tope de reembolso es **5.000** |
| `70042` | Saldo a favor alto (80.000) sin consumir |
| `70043` | **Debe**: saldo negativo, tope de reembolso en 0 (botón apagado) |
| `70044` | Ya tiene un reembolso parcial (abonó 40.000, se le devolvieron 16.000) |
| `70045` | Saldo exactamente en cero |

Para recargar el seed hay que limpiar a mano (es idempotente: si la persona `70001`
ya existe, no hace nada).

---

## Otros puntos abiertos que dependen de esta ronda

- **Reembolso con el número corregido por el back:** con la persona `70041` el tope
  debe quedar en 5.000, y pedir más debe dar 400 desde el servidor ("la persona solo
  tiene N disponible para devolver"), no solo recortarse en pantalla. El front ya no
  recorta nada por su cuenta: usa `refundable` tal cual.
- **Método de pago inactivo:** con BONO REGALO desactivado, comprobar que no aparece
  en el cobro, en el pago por adelantado ni en el reembolso. El selector ya usa
  `GET /api/metodos-pago?activo=true`, así que si aparece es que el filtro del
  servidor no está funcionando. Llamarlo directo a la API con ese método debe dar 400.
- **Cancelar una sesión:** con el seed, cancelar una sesión con check-ins y mirar que
  la fila de recepción, la pantalla de sala de espera (TV) y la lista de Sesiones —
  en otra pestaña — se actualicen solas, sin recargar.
- **Cambio de contraseña:** en el log del servidor debe aparecer la línea
  `Contraseña cambiada por el propio usuario ID ...`. Si no aparece, la petición no
  está llegando.

---

## Para el día del deploy en Cloudflare Pages

**Usar el método QR cuando se despliegue** (anotado el 20-sep).

El QR de la sala no lleva una dirección escrita a mano: codifica `origen-de-la-página + /member/home`,
así que al desplegar apunta solo a la dirección real de Cloudflare Pages, sin cambiar código. Lo que
sí hay que comprobar:

- **HTTPS y dirección real:** que el QR de `/lobby/qr` muestre la dirección desplegada y no
  `localhost`, y que al escanearlo desde un celular abra la pantalla de miembro.
- **Abrir `/member/home` directo** (sin pasar por el inicio): Cloudflare Pages debería servir la
  aplicación para rutas desconocidas mientras no haya un `404.html` en la raíz; comprobarlo, porque
  si no, el QR daría "página no encontrada".
- **Sin sesión iniciada:** el QR debe mandar a iniciar sesión y devolver a `/member/home` al
  terminar.
- **Conexión con el back:** que la variable `VITE_API_URL` apunte al backend desplegado al construir,
  que el CORS lo permita y que el WebSocket funcione por `wss` (la fila en vivo y la TV dependen de él).

---

## Verificaciones nuevas de esta ronda (20-sep)

- **Observaciones de salud:** guardar algo en `/emergency-contact`, recargar la página y comprobar
  que **siguen ahí**. (Era un defecto del frontend — la pantalla abría en la vista de "lista" y no
  mostraba lo guardado — y además, si se pulsaba Guardar sin darse cuenta, lo borraba.) Si tras el
  arreglo se siguen perdiendo al recargar, entonces sí es del back. Para saber de qué lado está:
  después de guardar, mirar en la **base** la columna de observaciones de salud de esa persona. Si
  el texto está ahí, el `GET /perfil` no lo está devolviendo; si no está, no se está guardando.
  Está pedido al back en `pendientes-back-20-09.md` (punto 8).
- **Miembro sin errores 403:** entrar como MIEMBRO y comprobar que en el log del back ya no aparece
  `Acceso denegado` por `/api/cola/admin`.
- **Login sin recargar:** iniciar sesión como recepción y comprobar que la lista de check-in aparece
  sola, sin tener que recargar la página ni hacer otra acción.

---

## Bloque B del 20-sep: qué probar (check-in, sesiones, profesor)

Con el seed cargado (`70001`–`70045`) y una cuenta de recepción, una de miembro y una de profesor.

> **Actualizado el 21-sep:** varios textos de esta lista cambiaron ("sesiones" en vez de "días", el encabezado con dos cifras, sin "turno" cuando la sesión ya está paga). Manda la sección del 21-sep de más abajo; esta queda como referencia de lo ya probado el 20-sep.

**Miembro (`/member/home`)**
- [ ] Marcar la llegada y **recargar (F5)**: debe seguir diciendo "Tu llegada de hoy está registrada" (con el turno si aún debe pagar; si la sesión ya está paga, solo "puedes pasar directo"), y no volver el botón.
- [ ] Cerrar sesión y volver a entrar: lo mismo.
- [ ] Marcar fuera de horario (sin clase en la próxima hora): debe salir el mensaje del servidor, "No hay ninguna clase programada en este momento".
- [ ] Cuando recepción confirma la asistencia, el mensaje pasa a "Tu asistencia de hoy está confirmada".

**Check-in de recepción (`/reception/checkin`)**
- [ ] Una llegada hecha por el miembro dice **"App / miembro"** y no "Terminal PSR". Una hecha por "Agregar manual" dice **"Manual"**.
- [ ] "Agregar manual" por documento funciona (usa el endpoint nuevo).
- [ ] Recibir más que la tarifa: aparecen **tres botones** — Entregar vuelto, Abonar N sesiones, Dejar vuelto pendiente.
- [ ] **Dejar vuelto pendiente:** la fila pasa a **azul** con "Vuelto por entregar $X" (antes decía "Se le debe", que se confundía con una deuda de la persona), y aparecen "Entregar vuelto" y "Abonar N sesiones". Probar los dos.
- [ ] Una persona con un día pagado por adelantado **no** aparece en "Saldo pendiente" (queda en "Al día"). Una que debe, en rojo.
- [ ] Tras cobrar, la fila muestra **"Recibido $X"** y los controles de cobro quedan escondidos detrás de "Registrar otro pago".
- [ ] El encabezado muestra **"Esta sesión" e "Ingresó hoy"** (ver la sección del 21-sep) y se actualiza al cobrar y al reembolsar. (Con una cuenta de profesor no debe mostrar cifras.)
- [ ] Botón grande **"Finalizar sesión"** junto a "Agregar Manual" solo cuando hay una sesión en curso; al confirmarlo, la sesión se cierra (no se borra).
- [ ] Con la sesión terminada aparece el aviso "No hay clase en curso", y en las filas ya no se ofrecen cobros.

**Sesiones (`/admin/sessions`)**
- [ ] Se ve **una semana de lunes a domingo**, con cada día como título y sus sesiones debajo; flechas para cambiar de semana y "Esta semana".
- [ ] El filtro de estado empieza en **Activas**; probar Canceladas y Todas.
- [ ] "Rango de fechas": elegir un rango pasado y aplicar.
- [ ] Una sesión **en curso** tiene botón "Finalizar"; una terminada ya no se puede cancelar.
- [ ] Cancelar o crear una sesión desde otra pestaña actualiza esta lista sola.

**Profesor (`/teacher/sessions`)**
- [ ] Al entrar con una cuenta de profesor, "Mis Sesiones" es la primera opción y la pantalla de inicio.
- [ ] Solo aparecen las sesiones asignadas a ese profesor, por semana.
- [ ] "Ver quién asistió" muestra la lista, incluidos los que llegaron y quedaron **"Sin confirmar"**.
- [ ] "Finalizar sesión" funciona en una suya en curso.

**Puntos donde puedo haber asumido mal la forma de los datos** (ver `pendientes-back-20-09-seguimiento.md`): el vuelto pendiente de una persona con varios cobros, y los totales de `check-ins / asistieron` en las tarjetas de sesión. Si algo sale en 0 o no encuentra el vuelto, es probable que sea por eso.

---

## Ronda del 21-sep: qué probar (check-in, cobro, tiempo real, sesiones)

**Preparación.** Seed cargado (`SEED_ENABLED` y `SEED_HISTORICAL_ENABLED`, ver arriba). Cuatro sesiones abiertas
a la vez: **recepción** (con DevTools abierto → pestaña Network → filtro WS), **admin**, **profesor** y un
**miembro** (celular o ventana de incógnito). Tarifa de los ejemplos: 5.000. Los números de los casos se
arman con la persona que corresponda (`70041`–`70045` del seed sirven para saldos; para crédito, abonarle
por adelantado desde recepción).

### 0. Antes de abrir el navegador (el back tiene SQL nuevo que nunca corrió contra Postgres)

El back avisa que lleva cuatro rondas acumulando SQL sin ejecutar: una migración V2, funciones nuevas, un
`DROP FUNCTION` que reemplaza otra en uso, columnas nuevas en dos vistas y un seeder con `INSERT` directos.
Si algo falla en esta ronda, casi seguro empieza ahí. En este orden:

1. **Arrancar el back contra Postgres y leer el log.** Errores de la migración V2 o del seeder = no seguir.
2. **API cruda** (Postman/curl, con token de recepción): `GET /api/cola/resumen`, `GET /api/cola/admin`,
   `GET /api/sesiones?desde=…&hasta=…` y `GET /api/checkin/propio` (con token de miembro). Un 500 en cualquiera
   es SQL, no pantalla: mandar cuerpo y log al back. Comprobar que existen los campos nuevos: `amountDue`,
   `coveredByCredit` (cola); `session`, `collectedThisSession`, `collectedToday` (resumen); `teacherName`,
   `totalCheckins`, `totalAttended`, `canClose`, `canCancel` (sesiones); `paymentStatus`, `amountDue` (check-in propio).
3. **Mirar el `amountDue` de tres filas antes de cobrar:** una con deuda, una con crédito y una sin saldo. Es la
   cifra con la que se cobra y el back solo la razonó, no la ejecutó.

### A. Tiempo real (lo más importante: era lo que estaba roto)

- [ ] **Llegada propia.** Recepción abierta y quieta. Un miembro marca su llegada: la fila aparece **en menos
      de ~5 s sin tocar nada**. Repetir con tres personas distintas. Si tarda 10–30 s, no es la señal sino el
      relleno de 30 s (`mejoras-v2.md` F1): anotarlo, significa que el WebSocket no está llegando. En DevTools →
      WS debe verse un mensaje en el momento de marcar.
- [ ] **La misma persona marca dos veces:** sigue habiendo una sola fila y no cambia el turno.
- [ ] **Sesión que empieza por el reloj.** Con una sesión programada dentro de unos minutos: la cabecera pasa de
      "Próxima clase" a **"Clase en curso … · termina hh:mm"** y aparece el botón "Finalizar sesión" en **menos de
      ~1 minuto** sin recargar, en admin, recepción y profesor a la vez.
- [ ] **Sesión que termina por el reloj.** En menos de ~1 minuto el botón desaparece, la lista queda vacía con
      "No hay clase en curso" y los contadores (Todos / Sin atender / Saldo pendiente / Al día) quedan en 0.
- [ ] **Dos clases seguidas** (por ejemplo 11:20–11:30 y 11:40–11:50): al empezar la segunda, "Finalizar sesión"
      muestra **la segunda** en su confirmación, no la primera. Es el caso que falló.
- [ ] **Sin conexión en vivo:** con la pestaña WS bloqueada, la pantalla igual se actualiza en ≤30 s.

### B. Cobro y crédito (tarifa 5.000)

Para cada caso: cobrar, mirar la fila, y **recargar (F5)** para confirmar que lo que se ve es lo que dejó el servidor.

| # | Punto de partida | Se recibe | Debe quedar |
|---|---|---|---|
| 1 | Sin saldo | 5.000 | "Sesión pagada", sin deuda ni vuelto |
| 2 | Sin saldo | 7.000, "Dejar vuelto pendiente" | Sesión pagada + azul **"Vuelto por entregar $2.000"** (no es deuda) |
| 3 | Sin saldo | 7.000, "Entregar vuelto" | Sesión pagada, sin vuelto pendiente |
| 4 | Sin saldo | 10.000, "Abonar 1 sesión" | Sesión de hoy + 1 sesión a favor |
| 5 | **Debe 5.000** | 10.000 | Deuda saldada **y** sesión de hoy pagada |
| 6 | **Debe 5.000** | 5.000 | Deuda saldada; **falta pagar hoy 5.000** |
| 7 | **Debe 2.000** | 5.000 | Quedan por pagar 2.000 |
| 8 | Con vuelto pendiente 2.000 | "Abonar 0 sesiones" | Botón desactivado; sí se puede "Entregar vuelto" |

**Crédito (sesiones pagadas por adelantado) — confirmación automática:**

- [ ] **Crédito 5, marca llegada (celular):** en recepción la fila **nace ya confirmada y en "Al día"**, no en
      "Sin atender"; nadie tiene que confirmarla. Su perfil baja a **4** sesiones **sin recargar** y su pantalla
      dice "Tu sesión de hoy ya está paga — puedes pasar directo a la clase", **sin "turno #N"**.
- [ ] **Crédito 1:** queda en 0 y "Al día".
- [ ] **Sin crédito, o con deuda, o con abono parcial (1.000):** **no** se confirma sola; sale en "Sin atender"
      con lo que debe pagar (5.000 / 7.000 / 4.000).
- [ ] **Marcar dos veces la misma sesión:** descuenta **una** sola vez (crédito 5 → 4, no → 3).
- [ ] **Dos sesiones el mismo día** con crédito 5: una llegada a cada una → crédito 3.
- [ ] **Deshacer asistencia** sobre una fila confirmada sola: el crédito vuelve a 5. En el historial esa
      confirmación figura a nombre de **SISTEMA**.
- [ ] Una sesión **cancelada** no descuenta nada.

**Caso Ana Pérez (pago parcial antes de confirmar):**

- [ ] Sin saldo, recibir **1.000**: la columna Saldo dice **"Falta $4.000"** y debajo "Tiene $1.000 a favor · la
      sesión se descuenta al confirmar" (ya **no** "A favor $1.000"). El cobro muestra "Por pagar $4.000".
- [ ] Pagar los 4.000 restantes → "Sesión pagada". Confirmar → "Al día".
- [ ] Alternativa: con 1.000 recibidos, **confirmar sin completar** → pasa a **"Debe $4.000"** y aparece en
      "Saldo pendiente".

### C. Encabezado del check-in

- [ ] Muestra la clase vigente: **"Clase en curso hh:mm – hh:mm · termina hh:mm"**; sin clase en curso, "Próxima
      clase …" o "Última clase de hoy …"; con la fila vacía, el texto dice a qué hora empieza la próxima.
- [ ] Muestra **"Esta sesión: $X | Ingresó hoy: $Y"** y, al pasar el mouse, cada una se explica. Los números
      deben ser **idénticos** a `collectedThisSession` y `collectedToday` del JSON de `GET /api/cola/resumen`.
- [ ] Caso de referencia (dos sesiones hoy; A entrega 10.000 = 5.000 hoy + 5.000 de adelanto; B entrega 7.000 con
      2.000 de vuelto entregado; C entrega 5.000 y se le reembolsan; D, en la sesión anterior, 5.000): esta
      sesión **10.000**, hoy **20.000**.
- [ ] Un **adelanto** sube "Ingresó hoy" pero **no** "Esta sesión". Un **reembolso** baja "Ingresó hoy".
- [ ] Con cuenta de **profesor** no aparece ninguna cifra (no tiene reportes), pero sí la hora de fin.
- [ ] "Ingresó hoy" **no** tiene por qué coincidir con "RECAUDO · DÍA" de Reportes (esa es la fecha de la clase,
      esta es la del pago): es lo esperado, hay una diferencia si hubo adelantos.

### D. Sesiones (`/admin/sessions`, `/reception/sessions`, `/teacher/sessions`)

- [ ] El **nombre del profesor aparece de una vez** al recargar (ya no "…" ni "Sin asignar" un instante).
- [ ] Los totales "N check-ins / N asistieron" son los reales.
- [ ] **Finalizar** y **Cancelar** aparecen según el rol: admin y recepción en cualquiera; un profesor **solo en
      las suyas**, y nunca en una terminada o cancelada.
- [ ] **Programación de una semana:** el aviso azul aparece al mirar días después de hoy + 6; esos días dicen
      **"Aún sin programar"**. Un lunes no se ve el lunes siguiente; un viernes se ve media semana: es lo esperado.
- [ ] "Regenerar programación" (admin) responde cuántas creó y no duplica al repetirlo.
- [ ] Cancelar o crear desde otra pestaña actualiza la lista sola.

### E. Miembro (`/member/home`)

- [ ] Marcar la llegada y **recargar (F5)**: sigue "Tu llegada de hoy está registrada", no vuelve el botón.
- [ ] Con la sesión ya terminada: dice "Ya marcaste tu llegada hoy" y solo ofrece marcar **otra** sesión.
- [ ] Fuera de horario: sale el mensaje del servidor, "No hay ninguna clase programada en este momento".
- [ ] El contador dice **"Sesiones pagadas por adelantado"** (no "días").
- [ ] Sin llegada hoy (204) no aparece ningún mensaje de error.

### F. Roles y cosas que ya se sabe

- [ ] Recepción, admin y profesor ven el check-in; el profesor no ve cifras de recaudo.
- **Conocido (no es un fallo nuevo):** en el check-in, un profesor puede ver "Finalizar sesión" en la clase de otro
  profesor y recibir 403 al pulsarlo (pedido al back: `canClose` en el resumen).
- **Va para el segundo lanzamiento (`mejoras-v2.md`):** paginación de Reportes con filtro, cobrar con monto 0 y
  "Finalizar" solo para el profesor que da la clase.

### Si algo falla, anotar

Rol, hora exacta, qué se esperaba y qué pasó, y de la pestaña **Network** la petición y la respuesta (estado y
cuerpo) de `GET /api/cola/admin`, `GET /api/cola/resumen` y, si es de la pantalla del miembro,
`GET /api/checkin/propio`. Con eso se sabe en un minuto si es el back o el front.
