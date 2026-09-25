# Notas para Backend — Tratamiento de Datos Personales (Habeas Data)

Respaldo en texto plano de lo que se envió al equipo de backend como
memo (publicado aparte, fuera de este repo). El frontend ya implementó
su parte de la política (ver `pendiente-implementar-sistema.md`);
esto es lo que queda del lado de backend.

**Marco:** Ley 1581 de 2012 y Decreto 1377 de 2013.

---

## 1. Registrar el consentimiento — resuelto (17-sep-2026)

Backend definió el contrato completo en `NOTAS-PARA-FRONTEND.md` (campo en el body
de cada endpoint, no un endpoint aparte) y ya lo está validando. **Frontend ya lo
conectó** (commit `ed318fb`, 17-sep-2026): los 4 campos viajan desde `autoregistro`,
`PersonFormModal` (alta manual), observaciones de salud y contactos de emergencia.
Este punto ya no es una pregunta abierta — ver el punto 1-bis, que es la única cosa
nueva que sale de esa respuesta.

**FYI, no es una pregunta:** para salud, el frontend nunca vuelve a mostrarle el
checkbox a la persona una vez que ya hay algo guardado (infiere que el consentimiento
ya existe porque, si no existiera, el backend no habría aceptado guardar ese
contenido la primera vez). Pero en cada `PUT` con contenido no vacío —primera vez o
la número cien— sigue mandando `healthDataConsent: true` en el body, tal como pide
el contrato. Solo cambia si la persona ve o no el checkbox; lo que llega al servidor
es siempre lo mismo.

## 1-bis. Trazabilidad 1:N del consentimiento de terceros — pregunta abierta de modelo de datos

`POST /api/personas/{id}/contactos-emergencia` ya exige `thirdPartyConsent: true` en
**cada** llamada, sin excepción — eso está bien y no se toca. La pregunta es sobre
cómo queda guardada esa fila una vez creada.

Cada contacto de emergencia es una persona real distinta. Si la fila de consentimiento
que se crea al aceptar `thirdPartyConsent` no queda vinculada a **qué contacto en
particular** autorizó (por ejemplo, un `contactId` de referencia), entonces el
historial de `GET /api/personas/{id}/consentimientos` para `THIRD_PARTY_DATA` se ve
como N filas idénticas — "otorgado el día X", "otorgado el día Y" — sin poder decir
cuál corresponde a cuál persona nombrada.

Eso no es un problema de validación (la validación ya funciona bien), es un problema
de **prueba**: el numeral 10 le reconoce a la persona el derecho a saber qué autorizó,
y si algún día hay que responder "¿en qué momento autorizaste dar los datos de
[nombre]?", la fila sin ese vínculo no puede contestarlo.

¿Vale la pena que cada fila de consentimiento de tipo `THIRD_PARTY_DATA` guarde a qué
contacto corresponde? Si la respuesta es sí, probablemente valga la pena aplicar el
mismo criterio (vincular la fila a la entidad concreta que autoriza, no solo al tipo)
a los demás tipos de consentimiento, por consistencia — decisión de ustedes, no la
estamos proponiendo como solución, solo señalando el hueco.

No bloquea nada del lado de frontend: seguimos mandando el campo en cada request tal
como está definido, esto es solo sobre cómo se archiva del otro lado.

---

## 2. Canal de habeas data — sin definir

El botón de "corrección o supresión" que ya vive en el perfil de cada
persona hoy solo abre un `mailto:` al correo de contacto — porque no
hay ni endpoint ni proceso manual definido de a quién le llega esa
solicitud, ni quién la atiende. Mientras eso no se decida, el
frontend se queda así a propósito: un formulario que aparentara
someterse a un proceso inexistente sería peor que no tenerlo.

### Qué falta decidir

- ¿Quién en el equipo recibe y resuelve una solicitud de este tipo?
- ¿Vale la pena un endpoint que registre la solicitud (con plazo de
  respuesta, ver Ley 1581), o el correo reactivo es suficiente por
  ahora?

---

## 3. Saneamiento de auditoría — a decidir

La auditoría de cambios conserva valores personales antiguos como
texto libre — por ejemplo, un teléfono corregido deja el valor
anterior guardado para siempre. Eso choca con una eventual solicitud
de supresión: la persona puede pedir que se borre su dato, pero
seguiría viviendo dentro del historial de auditoría.

### Qué falta decidir

- ¿Se deja tal cual (la auditoría prevalece con fines de seguridad),
  o se sanea el valor histórico cuando hay una solicitud de
  supresión aprobada?

---

## Dos decisiones de negocio que también tocan el diseño de backend

**A. Proveedores tecnológicos reales.** Aplazado a propósito hasta
tener el sistema terminado (VPS tipo Hostinger vs. un PaaS tipo
Railway, según lo que consuma el backend real). La sección de
transferencia internacional de la política se completa cuando esto se
resuelva.

**B. Archivo de los formatos físicos firmados.** El registro manual
usa un formato en papel firmado en vez del checkbox digital. Falta
decidir quién es responsable de archivarlos, dónde se guardan
físicamente y por cuánto tiempo.

---

El punto 1-bis no bloquea al frontend — seguimos conectando el contrato tal como está
definido hoy. Es una pregunta para cuando el equipo de backend tenga espacio, no antes.
