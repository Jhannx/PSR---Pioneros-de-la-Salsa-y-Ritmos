# Seguimiento para Backend — tras conectar el Bloque 4

Salió de conectar el frontend a lo que respondieron en el Bloque 4 de
`NOTAS-PARA-FRONTEND.md`. Son cosas chicas: dudas de forma de los datos y una
orquestación que hoy tiene que hacer el frontend y debería vivir en el servidor.

Todo sale de lo observado al conectar, no de leer su código.

---

## 1. Vuelto pendiente: acciones por persona, no por movimiento

**Qué pasa hoy.** Las dos acciones que crearon (`POST /api/movimientos/{id}/vuelto` y
`POST /api/movimientos/{id}/abonar-vuelto`) van **por movimiento**, pero la fila de la cola
solo trae el **total de vuelto pendiente de la persona** (`pendingChange`). Para resolver
un vuelto desde la fila, el frontend tiene que:

1. pedir `GET /api/movimientos/pendientes/vueltos`,
2. buscar ahí los movimientos de esa persona,
3. y repartir el monto entre ellos (del más antiguo al más nuevo), llamando a un
   movimiento tras otro.

Eso es lógica de negocio en el frontend (el mismo tipo de parche que ya se corrigió con el
reembolso), y es frágil: si la persona tiene vuelto pendiente de varios días, hay que
repartir el abono entre varios cobros.

**Qué se pide.** Lo más simple, dos acciones **por persona** en vez de por movimiento:

```
POST /api/personas/{id}/entregar-vuelto        → salda todo su vuelto pendiente
POST /api/personas/{id}/abonar-vuelto          { "amount": 46000, "note": "..." }
```

Que el servidor reparta entre los movimientos con vuelto sin entregar (del más antiguo al
más nuevo) y responda 400 si piden más de lo pendiente. Las de por movimiento pueden
quedarse como están.

## 2. Forma de `GET /api/movimientos/pendientes/vueltos`

**Qué se necesita saber.** ¿Cada elemento trae el **`personId`**? Hoy el frontend solo
tiene la seguridad de que trae `movementId`, `fullName`, `amountReceived` y `amountApplied`
(es lo que ya usaba la pestaña de administración). Sin `personId`, la búsqueda de arriba
cae a comparar el nombre completo, y dos personas con el mismo nombre se confundirían.
Si no lo trae, que lo traiga (o que se resuelva con el punto 1).

## 3. Resúmenes y "mis sesiones": dos detalles de forma

- **`GET /api/sesiones/resumenes`:** ¿el campo con el id de la sesión se llama
  `sessionId` o `id`? El frontend acepta cualquiera de los dos para cruzar los totales
  (`totalCheckins` / `totalAttended`) con la lista, pero conviene confirmarlo.
- **`GET /api/sesiones/mias`:** ¿acepta también el parámetro `estado` (activas por defecto,
  canceladas, todas)? Hoy el frontend **no lo manda** y el profesor ve todas las que
  devuelva, con su `status`. Si `/mias` devuelve canceladas por defecto, díganme, porque
  quizá convenga que use el mismo `estado=activas` que `/api/sesiones`.

## 4. Para tener presente (sin acción)

- **"Recaudo de hoy":** el encabezado del check-in ahora usa
  `GET /api/reportes/recaudo-diario` para hoy, como sugirieron. Un **PROFESOR** no tiene
  reportes de ingresos, así que ahí responde 403 y el encabezado simplemente **no muestra
  la cifra** (no muestra un cero). Si quieren que el profesor la vea, avísenme.
- **`POST /api/checkin/manual/documento`:** el frontend ignora el cuerpo de la respuesta
  (refresca la cola después), así que cualquier forma le sirve.
