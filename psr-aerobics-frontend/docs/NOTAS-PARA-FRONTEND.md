# Notas para Frontend

---

# Registro de consentimiento — contrato listo (17-sep-2026)

Esto responde al punto 1 de `notas-backend-habeas-data.md` y al punto 7 de
`pendiente-implementar-sistema.md`: **el contrato ya existe y el backend ya lo valida**. Las cuatro
casillas que hoy son candado de interfaz tienen por fin a dónde mandar su estado.

## La decisión de contrato, y por qué

Las notas planteaban dos opciones: un campo por casilla en el body de cada endpoint, o un endpoint de
consentimiento aparte llamado justo antes o justo después de la acción.

**Es un campo en el body de cada endpoint.** La segunda opción reintroduce el mismo agujero que todo
esto vino a tapar: dos llamadas separadas se pueden partir por la mitad, y entonces queda una persona
sin constancia o una constancia sin persona. Con el campo en el body, el consentimiento se graba
dentro de la misma transacción que la operación que lo motiva — o quedan las dos cosas, o no queda
ninguna.

**No hay que llamar a ningún endpoint extra en los flujos normales.** Basta con añadir el campo.

## Qué añadir a cada petición

### 1. Autoregistro — `POST /api/personas/autoregistro`

Las dos casillas de `RegistrationConsent.jsx` (`SelfRegistrationPage`, `LobbyRegistrationPage`):

```jsonc
{
  // ...lo que ya manda
  "dataProcessingConsent": true,         // numeral 4.1 — autorización general
  "voluntaryParticipationConsent": true  // numeral 12  — participación voluntaria
}
```

Cualquiera de los dos en `false`, `null` o ausente → **400**, con un mensaje distinto para cada uno
para que se pueda señalar la casilla que falta.

### 2. Alta manual de recepción — `POST /api/personas`

**Esto sí pide un cambio en `PersonFormModal.jsx`, y es a propósito.** La decisión de alcance de que
ahí no hubiera casilla se quedó corta: sin ningún campo, la mitad de las altas quedaba sin fila de
consentimiento y el registro entero dejaba de servir como prueba.

```jsonc
{
  // ...lo que ya manda
  "signedPhysicalConsent": true   // "se recogió el formato físico de autorización firmado"
}
```

No es la casilla digital de la política: es la confirmación de quien atiende el mostrador de que tiene
el papel firmado delante. Cubre los dos consentimientos anteriores de una vez, porque el formato lleva
los dos textos. Sin él → **400**.

El consentimiento queda grabado con medio `FISICO` y a nombre de la cuenta que registra.

### 3. Observaciones de salud — `PUT /api/personas/{id}/observaciones-salud`

```jsonc
{
  "healthObservations": "...",
  "healthDataConsent": true      // numeral 4.2
}
```

La regla es **asimétrica**, igual que la que ya implementasteis:

| Caso | `healthDataConsent` |
|---|---|
| Guardar contenido | obligatorio `true`, si no → **400** |
| Vaciar el campo (`null` o `""`) | se ignora, mandad lo que mandéis |

Vaciar el campo **revoca** además el consentimiento de salud. Es intencionado: retirar el dato y
retirar el permiso son el mismo gesto.

### 4. Contacto de emergencia — `POST /api/personas/{id}/contactos-emergencia`

```jsonc
{
  "name": "...",
  "phone": "...",
  "thirdPartyConsent": true      // numeral 5
}
```

Sin él → **400**.

**Solo en el POST.** `PUT /api/contactos-emergencia/{contactId}` no lo pide y lo ignora si llega:
corregir un dígito mal tecleado no es suministrar los datos de nadie nuevo, y volver a pedir la
declaración en cada arreglo la convertiría en un trámite que se marca sin leer.

Ojo: se graba **una fila por contacto**, no una por persona. Cada contacto es un tercero distinto, así
que la casilla hay que pedirla cada vez que se añade uno.

## Consultar el estado — `GET /api/personas/{id}/consentimientos`

Permisos: personal, o la propia persona. Útil para "Mis Datos" del perfil — el numeral 10 le reconoce
el derecho a saber qué autorizó y cuándo.

```jsonc
{
  "personId": 42,
  "policyVersion": "1.0",
  "dataProcessing": true,
  "voluntaryParticipation": true,
  "healthData": false,
  "thirdPartyData": true,
  "registrationConsentMissing": false,
  "history": [ /* todas las filas, de la más reciente a la más antigua */ ]
}
```

**`registrationConsentMissing` es el campo que os hace falta mirar.** Es `true` cuando la persona no
tiene *ninguna* fila de autorización general — el caso de todo el mundo que ya estaba registrado antes
de que esto existiera. No se rellenaron esas filas en la migración y no deben rellenarse nunca:
inventar un consentimiento que nadie dio es peor que no tenerlo.

Lo natural sería que, con ese flag en `true`, la app pida las dos casillas en el siguiente acceso y las
mande al endpoint de abajo. **No es urgente y no bloquea nada** — decidid vosotros cuándo meterlo.

## Registrar un consentimiento suelto — `POST /api/personas/{id}/consentimientos`

Solo para los dos casos que no tienen operación propia: **preguntarle a quien ya estaba registrado**, y
**revocar** más tarde. En el flujo normal no se usa.

```jsonc
{
  "type": "DATA_PROCESSING",   // DATA_PROCESSING | VOLUNTARY_PARTICIPATION | HEALTH_DATA | THIRD_PARTY_DATA
  "granted": true,             // false = revocación
  "medium": "DIGITAL"          // DIGITAL | PHYSICAL
}
```

Devuelve **201** siempre: nunca actualiza una fila anterior. Revocar y volver a otorgar deja las dos
filas, porque las dos ocurrieron.

Un aviso sobre revocar: **revocar `DATA_PROCESSING` no borra nada**. Equivale a una solicitud de
supresión, y eso el sistema no lo puede ejecutar solo — hay movimientos de dinero con obligaciones
contables detrás (numeral 8.1, diez años). Se atiende por el canal del numeral 10. Si ponéis un botón
de revocar, que el texto no prometa un borrado que no va a ocurrir. Revocar `HEALTH_DATA` sí borra las
observaciones de salud, esa sí es inmediata.

## Dos cosas que decidió el backend y conviene que sepáis

**No se guarda IP ni user-agent.** Serían evidencia barata, pero el numeral 2.8 de la política ya
publicada declara qué recoge el sistema y la IP no está en esa lista. Añadir un dato personal no
declarado justo en la tabla que existe para demostrar cumplimiento es la contradicción más cara
posible. Lo que identifica el acto es qué cuenta lo afirmó.

**La validación del backend no sustituye vuestro candado, lo respalda.** Seguíd bloqueando el botón:
es mejor experiencia que un 400. Lo que cambia es que ahora saltarse la app llamando a la API
directamente tampoco funciona.

## Lo que sigue sin existir

El canal de habeas data (punto 2 de `notas-backend-habeas-data.md`) sigue sin endpoint ni proceso
definido — es una decisión de negocio, no de código. El `mailto:` de `UserProfilePage.jsx` se queda
como está hasta que se decida quién atiende esas solicitudes.
