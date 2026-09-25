# Pendientes — después del MVP

Lo que está fuera del MVP y por qué. No es una lista de deseos: cada punto dice qué hay ya en el
código y qué falta exactamente, para que retomarlo no obligue a redescubrirlo.

---

## 1. Adjuntos en correo — **la fontanería está hecha, falta el PDF y engancharlo**

**Estado: capacidad construida y sin usar.** No rompe nada y no se ejecuta en ningún flujo. Se dejó
así a propósito el 13-sep, con el MVP encima: terminarlo exigía generar y versionar el PDF de términos
y condiciones, y eso no cabía.

### Lo que YA está en el código

| Pieza | Archivo | Estado |
|---|---|---|
| Modelo del adjunto | `email/EmailAttachment.java` | ✅ nuevo. `filename`, `contentType`, `content` (byte[]), con validación en el constructor compacto |
| Contrato | `email/EmailSender.java` | ✅ `send(...)` de 5 argumentos con la lista de adjuntos. El de 4 quedó como `default` que delega con lista vacía |
| SMTP | `email/SmtpEmailSender.java` | ✅ `MimeMessageHelper` en multipart **solo si hay adjuntos**, y `addAttachment` con `ByteArrayResource` |
| Brevo | `email/BrevoApiEmailSender.java` | ✅ campo `attachment` en base64 dentro del JSON, y `null` —no lista vacía— cuando no hay adjuntos |

**Nadie llama al método de 5 argumentos todavía.** Los cuatro correos del sistema siguen usando el de
siempre, sin tocar una línea. Por eso esto puede quedarse tal cual indefinidamente sin coste.

### Lo que falta

1. **El PDF de términos y condiciones.** No existe. Hay que decidir si se guarda como recurso en
   `src/main/resources/` o se genera. Es la razón real por la que esto no se terminó.
2. **Una pieza que lo cargue**, idealmente una sola vez al arrancar y cacheado en memoria — el envío
   ocurre después del commit y en otro hilo, así que leer del disco en cada correo es pedir problemas.
3. **Engancharlo a `EmailService.sendWelcome`**, pasando la lista de adjuntos al `send` de 5
   argumentos.
4. **Un interruptor de configuración** (`app.email.terms.enabled`) para poder apagarlo sin desplegar.

### Dos avisos para quien lo retome

- **El adjunto va entero en memoria.** Para unos cientos de KB da igual; si algún día hay que mandar
  algo grande, `EmailAttachment` es lo que habría que cambiar. Está explicado en su javadoc.
- **Brevo rechaza `"attachment": []` con un 400.** Por eso la implementación manda `null` cuando no
  hay adjuntos y el record lleva `@JsonInclude(NON_NULL)`. No "simplificar" eso a una lista vacía.

### Y una alternativa que puede ser mejor que el adjunto

El frontend propuso publicar los términos como una página suya y mandar el enlace. Es mejor incluso
sin la prisa: el correo no engorda, una errata se corrige sin reenviar nada a nadie, y se esquivan los
filtros de spam que castigan los adjuntos.

**Si lo que la parte legal necesita es constancia de aceptación, el adjunto no lo resuelve.** Eso
era una tabla y un endpoint, y **ya está hecho** (17-sep): ver `docs/NOTAS-PARA-FRONTEND.md` y el
punto 7 de `docs/pendiente-implementar-sistema.md`. Lo cual refuerza la alternativa del enlace: si la
constancia vive en `consentimiento`, el correo no necesita cargar con el PDF.

---

## 2. Endpoint de reembolso — **ya no está pendiente, se hizo el 13-sep**

Se deja anotado aquí solo para que no se busque: `POST /api/personas/{personId}/reembolso` existe,
reparte FIFO entre los abonos con saldo y va en una sola transacción.

De paso salió un defecto de dinero que sí era grave: `sp_registrar_reembolso` comparaba el tope contra
el abono original **sin restar lo ya reembolsado**, así que un mismo abono se podía devolver varias
veces completo. Corregido en el mismo cambio.

---

## 3. Lo que viene de rondas anteriores y sigue abierto

| Qué | Dónde quedó |
|---|---|
| **Refresh token**, todo el Java | La tabla `refresh_token` está en `V1` desde el 10-sep. No hay ni una clase |
| `GET /api/personas/{id}/cambio-correo-pendiente` | Sin mapping en `PersonController` |
| **Seed histórico** (10 días de sesiones, pagos y asistencias) | Sin él no se puede medir rendimiento con datos reales |
| **Medir el N+1** con `hibernate.generate_statistics` | Nunca se hizo. Ver abajo |
| Índice trigram para `unaccent()` en las búsquedas | Sin medir. Exigiría un `V2`: `V1` ya está aplicado |

Sobre los dos últimos: el 13-sep se quitó la causa estructural más probable de la lentitud de "Roles y
Accesos" —el listado de usuarios no traía los roles y obligaba a pedirlos uno por uno—. **Si después de
eso sigue lenta, entonces sí toca medir**, y solo entonces decidir el índice.

Queda además una decisión de diseño sin tomar: **construir las autoridades desde los claims del JWT**
en vez de consultar la base en cada petición. Hoy cada llamada autenticada de toda la API vuelve a
traer usuario + roles + persona. El coste de cambiarlo es que una cuenta desactivada seguiría entrando
hasta que caduque su token (1 h). Es una decisión de producto, no una optimización.
