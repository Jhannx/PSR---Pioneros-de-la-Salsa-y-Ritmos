# Pendientes para Backend — ronda del 21-sep (a 6 h del deploy)

Solo cambios que se piden al back. Cada punto trae: lo que se vio, los casos exactos con sus números, lo
que se pide y cómo se sabe que quedó bien. Cada uno reemplaza un parche del frontend (anotado en
`mejoras-v2.md`, punto 10, bloque F).

**Sobre los números de los ejemplos:** 5.000 es la tarifa que hoy muestra el encabezado del check-in y la
que salió en las pruebas; se usa **solo para que los casos tengan cifras**, el servidor debe usar siempre
la tarifa vigente. "Crédito" = sesiones pagadas por adelantado (saldo a favor ÷ tarifa). Las tres vías de
check-in son `POST /api/checkin/propio`, `POST /api/checkin/manual/documento` y la terminal por documento.

**De dónde sale cada caso:** los marcados **(reportado)** ocurrieron en las pruebas del 21-sep. Los
marcados **(borde)** los agregué yo para que no se rompan al implementar; no se han visto fallar.

Orden por impacto en el día del deploy: 1 y 2 primero.

---

## 1. Señales en tiempo real que no llegan

### Qué se vio

**Caso 1A (reportado) — llegada propia que no se ve en recepción** (funcionaba antes de la ronda del 20-09; dejó de
funcionar con ella):

1. Recepción tiene abierto el check-in de la sesión en curso (lista vacía o con algunas personas).
2. Un miembro, desde su celular, llama a `POST /api/checkin/propio` y la llegada se registra sin error.
3. La lista de recepción **no cambia**, ni tras esperar varios minutos.
4. Recepción agrega a otra persona con `POST /api/checkin/manual/documento` → en ese momento la lista
   sí se recarga y aparecen **todas** las llegadas propias anteriores.
5. Una nueva llegada propia vuelve a no aparecer hasta que se agrega a alguien a mano.

Conclusión: la lectura de la cola (`GET /api/cola/admin`) devuelve bien los datos; lo que falla es que el
frontend no se entera de que hay una llegada nueva.

**Caso 1B (reportado) — sesión que empieza o termina por el reloj:** el estado de una sesión (`UPCOMING`,
`IN_PROGRESS`, `ENDED`) cambia solo con la hora y no llega ninguna señal en ese momento. Resultado real:
un admin abrió la pantalla con la clase de 11:20–11:30 en curso y siguió viendo "Finalizar sesión" mucho
después de que terminó; un profesor y recepción, que abrieron la pantalla cuando aún no había clase en
curso, nunca vieron el botón cuando la clase de 11:40 empezó.

### Se pide

- **1A:** que `POST /api/checkin/propio` publique la señal de cambio de cola en `/topic/cola/admin`
  **después de confirmar (commit) la transacción**. Publicarla antes hace que el frontend relea y todavía
  no vea la llegada. Igual para `/manual/documento` y la terminal por documento.
- **1B:** que se publique `SESSION_CHANGED` (mismo tema, `{sessionId, type}`) **cuando una sesión empieza
  y cuando termina por el reloj**, además de cuando se cierra a mano, se crea o se cancela.

El frontend relee por REST con cualquier señal; no necesita datos en el mensaje.

### Cómo se sabe que quedó bien

- Con recepción abierta y sin tocar nada, una llegada propia aparece en la lista en menos de 2 segundos.
- Con una sesión programada para las 11:40, a las 11:40:00 llega una señal `SESSION_CHANGED` con su
  `sessionId`; a las 11:50:00 llega otra.

Mientras tanto el frontend relee la cola y las sesiones cada ~10 s.

---

## 2. Confirmar la asistencia cuando la sesión ya está cubierta

### Qué se vio

**Caso 2A (reportado) — miembro con crédito:** una persona con **5 sesiones pagadas por adelantado** (crédito 5)
marca su llegada con `POST /api/checkin/propio`. La respuesta le indica que puede pasar directo (su
`amountDue` es 0). Pero en recepción su fila queda con `attended: false` y aparece en el filtro **"Sin
atender"**, y quien recibe tiene que confirmarle la asistencia a mano. No debería: la sesión ya está
pagada, no hay nada que hacer con ella.

### Se pide

Al registrar una llegada (las tres vías), si la persona **no debe nada por hoy** (su `amountDue` es 0
porque el crédito cubre la sesión), el servidor debe **confirmar la asistencia y descontar esa sesión del
crédito en ese mismo momento**.

Resultado esperado de la fila y la cuenta (la primera fila es lo reportado; las demás son bordes):

| Punto de partida | Después de la llegada |
|---|---|
| Crédito 5 (saldo a favor 25.000) | `attended: true`, crédito 4 (saldo a favor 20.000), `accountStatus: AL_DIA`, `amountCharged: 5.000` |
| Crédito 1 (saldo a favor 5.000) | `attended: true`, crédito 0 (saldo 0), `AL_DIA` |
| Crédito 0, sin deuda | **No** se confirma (`attended: false`, `amountDue: 5.000`) — recepción cobra y confirma |
| Abonó 1.000 hoy (saldo a favor 1.000) | **No** se confirma (`amountDue: 4.000`) |
| Debe 2.000 (saldo −2.000) | **No** se confirma (`amountDue: 7.000`) |

Casos límite **(borde)** que no deben romperse:

- **Idempotencia:** si la persona marca dos veces la llegada a la **misma** sesión, la sesión se
  descuenta **una sola vez**.
- **Dos sesiones en el mismo día:** con crédito 5 y dos clases hoy, la llegada a cada sesión descuenta
  una (crédito 3 al final). Una llegada a la segunda no reutiliza el descuento de la primera.
- **Deshacer:** si recepción usa "deshacer asistencia" sobre una fila confirmada automáticamente, la
  sesión vuelve al crédito (crédito 5 otra vez), igual que ya ocurre con una confirmación manual.
- **Una sesión cancelada** no descuenta nada.

### Cómo se sabe que quedó bien

Persona con crédito 5 marca llegada → sin ninguna acción de recepción, su fila está en "Al día", no en
"Sin atender", y su perfil dice 4 sesiones a favor.

---

## 3. Dos cifras de recaudo, con nombre y definición

### Qué se vio

El encabezado del check-in muestra una sola cifra, "Recaudo de hoy", y recepción no distingue si es lo
cobrado en la clase que está dando o todo lo que ingresó en el día (incluidos adelantos para otras
clases). El frontend hoy la toma del dashboard (`totals.day.revenue`) y, para la de la sesión, tendría que
combinar dos reportes y escoger una fila: eso es lógica que no debe estar en el cliente.

### Caso para fijar la definición (borde: armado para que la definición quede sin ambigüedad; lo reportado es solo que la cifra confunde)

Hoy hay dos sesiones. En la **sesión 1** (vigente):

- Persona A entrega 10.000: 5.000 para hoy y 5.000 de **adelanto** (1 sesión futura).
- Persona B entrega 7.000: 5.000 aplicados y **2.000 de vuelto entregado**.
- Persona C entrega 5.000 y luego se le **reembolsa** 5.000 de saldo a favor.

En la **sesión 2** (ya terminada, esta mañana): persona D entregó 5.000 aplicados.

### Se pide

Una respuesta con las dos cifras ya calculadas y su definición escrita. Propuesta (el back decide y la
deja documentada):

- `collectedThisSession`: lo aplicado a la sesión vigente (sesión 1: A 5.000 + B 5.000 = **10.000**).
- `collectedToday`: todo lo aplicado hoy en todas las sesiones, incluyendo adelantos, sin contar el vuelto
  entregado y **restando reembolsos** (A 10.000 + B 5.000 + C 0 + D 5.000 = **20.000**).

Si la definición correcta es otra, lo importante es que quede escrita y que la respuesta entregue esas dos
cifras. Confirmar el resultado del caso anterior con la definición elegida.

Ubicación: una sola respuesta corta, por ejemplo `GET /api/cola/resumen` (ver punto 4, que puede ir en la
misma), permitida a ADMIN, RECEPCIÓN y PROFESOR. Un profesor puede recibir las cifras en `null`: el
frontend no muestra nada cuando vienen así.

### Cómo se sabe que quedó bien

Con el caso anterior cargado, las dos cifras dan 10.000 y 20.000 (o las de la definición elegida) y el
frontend las muestra tal cual, sin sumar ni restar nada.

---

## 4. La sesión vigente, resuelta por el servidor

### Qué se vio

**Caso 4A (reportado) — "Finalizar sesión" apunta a la sesión equivocada:** estando la clase de 11:20–11:30 ya terminada y la
de 11:40–11:50 en curso (la hora exacta de la prueba no se anotó; 11:42 es un ejemplo), al pulsar "Finalizar sesión" en recepción, la confirmación mostró "11:20 a. m. – 11:30 a. m." y no la sesión
en curso real. Si se hubiera confirmado, habría intentado cerrar una sesión ya terminada
(`PUT /api/sesiones/{id}/cerrar` en una sesión terminada no hace nada) y la real habría seguido abierta. El
frontend elige esa sesión de una lista de sesiones que puede estar desactualizada.

**Caso 4B (borde) — clase en curso sin nadie en la fila:** con una sesión en curso y **cero llegadas**, la cola
responde `[]` y el frontend no tiene forma de saber cuál es la sesión en curso, su hora de fin ni su id,
para mostrar "termina a las 11:50" o permitir finalizarla.

### Se pide

Un dato con la sesión vigente completa, aunque no haya ninguna llegada:

```
GET /api/cola/resumen   (o GET /api/sesiones/vigente)
→ 200 {
    "session": { "id": 42, "startTime": "...", "endTime": "...", "status": "IN_PROGRESS", "teacherUserId": 7 },
    "collectedThisSession": 10000,
    "collectedToday": 20000
  }
→ "session": null  si no hay sesión vigente hoy
```

"Vigente" con la misma regla que ya usa `GET /api/cola/admin`: la que está en curso; si no hay, la próxima
del día; si no hay, la última del día. `status` con el mismo reloj del servidor.

### Cómo se sabe que quedó bien

A las 11:42 (sesiones 11:20–11:30 y 11:40–11:50) la respuesta trae la sesión de las 11:40 con
`IN_PROGRESS`, tenga o no llegadas. A las 11:55 trae la de las 11:40 con `ENDED` (si no hay otra más
tarde hoy).
