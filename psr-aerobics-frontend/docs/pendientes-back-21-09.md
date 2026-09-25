# Pendientes para Backend — seguimiento del 21-sep (tras la respuesta del back)

Los cuatro puntos del 21-sep (señales en tiempo real, confirmación automática por crédito, las dos cifras de
recaudo y la sesión vigente) quedaron atendidos y ya están conectados en el frontend. Lo único que se sigue
pidiendo, solo cambios:

---

## 1. `canClose` y `canCancel` en `GET /api/cola/resumen`

**Caso.** Un PROFESOR abre el check-in de recepción mientras la clase en curso es de **otro** profesor. El
resumen trae `session.canClose: null`, así que el frontend no puede saber si ese profesor puede finalizarla:
le muestra el botón "Finalizar sesión" (lo ven ADMIN, RECEPCIÓN y PROFESOR) y, al pulsarlo, recibe 403.

**Se pide.** Que `session.canClose` y `session.canCancel` vengan calculados en el resumen igual que en
`GET /api/sesiones`, según quién pregunta:

| Quién pregunta | Sesión en curso | `canClose` |
|---|---|---|
| ADMIN o RECEPCIÓN | cualquiera | `true` |
| PROFESOR | la suya | `true` |
| PROFESOR | de otro profesor | `false` |
| cualquiera | terminada o cancelada | `false` |

**Se sabe que quedó bien cuando** un profesor, en la clase de otro, ya no ve el botón "Finalizar sesión"
en el check-in.

## 2. El horizonte de la programación como dato

**Caso.** La pantalla de Sesiones avisa que la programación se genera con una semana de anticipación (hoy +
6 días). Ese "6" está escrito en el frontend.

**Se pide.** Que el horizonte llegue como dato en alguna respuesta de sesiones (por ejemplo
`horizonDays: 6`, o la fecha límite `generatedUntil`), para que el aviso no dependa de un número copiado. No
bloquea nada: es para quitar un parche (`mejoras-v2.md`, punto 10, E3).
