# Arquitectura del Frontend — PSR Aeróbicos

Documentación técnica del frontend (`psr-aerobics-frontend`): cómo está organizado el código, cómo
fluyen los datos entre pantalla y servidor, y qué responsabilidades ha ido asumiendo el cliente que,
estrictamente, le corresponden al backend. No documenta el backend ni decisiones de negocio — para
eso están `docs/NOTAS-PARA-FRONTEND.md` y `docs/pendiente-implementar-sistema.md` en esta misma carpeta.

## 1. Stack técnico

| Pieza | Elección | Notas |
|---|---|---|
| Framework | React 19 + Vite | SPA, sin SSR |
| Ruteo | `react-router-dom` v7 | rutas declaradas en `App.jsx` |
| Estilos | Tailwind v4 | clases utilitarias inline, sin CSS Modules |
| Estado global | React Context (`AuthContext`, `RoleContext`, `DataContext`) | sin Redux/Zustand |
| Tiempo real | STOMP sobre SockJS (`@stomp/stompjs`, `sockjs-client`) | un solo canal, la cola de recepción |
| HTTP | `fetch` envuelto a mano en `services/apiClient.js` | sin axios/react-query |
| Otros | `jspdf` (descarga de datos personales), `qrcode` (check-in por QR), `lucide-react` (íconos) | `jspdf` se importa dinámicamente, no entra al bundle inicial |

No hay capa de manejo de datos asíncronos tipo React Query/SWR: cada pantalla hace su propio
`useEffect` + `useState` + `try/catch` contra los servicios. Esto es consistente en todo el proyecto,
pero implica que patrones como "recargar tras escribir", "cancelar petición obsoleta" o "no pedir dos
veces lo mismo" se repiten a mano en cada componente en vez de vivir en una sola librería (ver §7.6
para un caso concreto).

## 2. Estructura de carpetas

```
src/
├── App.jsx              # Declaración de todas las rutas + selección de "home" por rol
├── main.jsx              # Punto de entrada de Vite/React
│
├── context/               # Estado global (ver §5)
│   ├── AuthContext.jsx     # Sesión: quién soy, token, login/logout
│   ├── RoleContext.jsx     # Rol ACTIVO cuando la cuenta tiene varios (presentación, no autorización)
│   ├── DataContext.jsx     # Datos de personal: personas, cola, sesiones + WebSocket
│   └── mockDataConstants.js # Shim de compatibilidad — reexporta constants/roles.js
│
├── services/               # Única capa que habla HTTP/WS con el backend (ver §6)
│   ├── apiClient.js         # fetch base, tokens, refresh proactivo, manejo de 401
│   ├── websocketService.js  # Cliente STOMP (topics de cola)
│   ├── authService.js, peopleService.js, queueService.js, sessionsService.js,
│   │   paymentsService.js, attendanceService.js, reportsService.js, userService.js
│
├── pages/                  # Una carpeta por rol/área — cada archivo es una ruta de App.jsx
│   ├── auth/                # Login, recuperar/restablecer contraseña, selector de rol
│   ├── public/               # Rutas sin sesión: autoregistro, política de privacidad
│   ├── lobby/                # Terminal PSR (kiosko de entrada): check-in, registro rápido, cola en TV, QR
│   ├── reception/             # Check-in de recepción, listado de personas
│   ├── admin/                 # Sesiones, horarios, reportes, auditoría, roles, configuración
│   ├── teacher/                # Agenda propia del profesor
│   └── member/                 # Panel del miembro: asistencia propia, contacto de emergencia, perfil
│
├── features/                # Lógica y componentes propios de un área, reutilizados entre pantallas
│   ├── reception/  (QueueRow, PersonFormModal, RefundModal, HardDeleteModal, PersonRow, utils.js)
│   ├── admin/       (PricingTab, RevenueTab, PendingChangeTab, PaymentMethodsTab, QueueConfigTab, utils.js)
│   ├── sessions/    (SessionCard, WeekNavigator, SessionStatusBadge, SessionDayBand, weekUtils.js)
│   └── lobby/       (utils.js)
│
├── components/               # Piezas genéricas, sin dueño de un solo feature
│   ├── auth/ProtectedRoute.jsx
│   ├── forms/ (BirthDateField, Field)
│   └── shared/ (RegistrationConsent, RegistrationHealthAndContacts, RegistrationConfirmModal, MissingConsentPanel)
│
├── layouts/AppLayout.jsx     # Sidebar + navegación por rol activo, envuelve todas las rutas protegidas
├── constants/ (roles.js, policyExcerpts.js)
├── hooks/useClock.js
└── utils/ (currency.js, date.js, text.js, validation.js, personalDataPdf.js)
```

**Criterio `pages/` vs `features/` vs `components/`** (implícito en el código, no escrito en ningún
sitio): `pages/` son las rutas en sí — orquestan estado y llaman servicios; `features/` agrupa lo que
una pantalla de esa área necesita (filas, modales, cálculos de presentación) pero que no es en sí una
ruta; `components/` es lo que cruza áreas (un campo de fecha, el guard de rutas). En la práctica el
límite no siempre se respeta: `ReceptionCheckinPage.jsx` (una page) contiene tanta lógica de negocio
como `QueueRow.jsx` (un feature) — ver §7.

## 3. Arquitectura en capas

```
┌─────────────────────────────────────────────────────────────────────┐
│  Pages (rutas)                                                       │
│  - dueñas del estado de pantalla (useState/useEffect)                │
│  - orquestan: piden datos, arman payloads, deciden qué mostrar       │
└───────────────┬───────────────────────────────────────┬─────────────┘
                │ usa                                    │ usa
┌───────────────▼──────────────┐          ┌──────────────▼─────────────┐
│  features/*  y  components/*  │          │  context/* (estado global)  │
│  - presentación + cálculos     │          │  - AuthContext (sesión)     │
│    derivados de props           │          │  - RoleContext (rol activo) │
│  - NO llaman servicios directo   │          │  - DataContext (personas/    │
│    salvo los modales de recepción │         │    cola/sesiones + WebSocket) │
└───────────────┬────────────────┘          └──────────────┬─────────────┘
                │                                           │ llama
                │                          ┌────────────────▼────────────┐
                └─────────────────────────▶│  services/*.js               │
                                            │  - un método por endpoint    │
                                            │  - sin lógica de negocio      │
                                            └────────────────┬─────────────┘
                                                              │ usa
                                            ┌────────────────▼────────────┐
                                            │  services/apiClient.js       │
                                            │  - fetch + Authorization      │
                                            │  - refresh de token, 401       │
                                            └────────────────┬─────────────┘
                                                              │ HTTP/WS
                                            ┌────────────────▼────────────┐
                                            │  Backend (Spring)             │
                                            └────────────────────────────┘
```

La regla que el propio código enuncia varias veces en comentarios (`weekUtils.js`, `reception/utils.js`,
`QueueRow.jsx`) es: **el servidor decide, el frontend solo pide y muestra**. Casi todos los cálculos de
negocio "de verdad" (saldo, estado de cuenta, monto a cobrar, quién puede cerrar una sesión) llegan ya
resueltos en el JSON de la API (`accountStatus`, `amountDue`, `refundable`, `canClose`…), precisamente
porque hubo bugs de producción cuando ese cálculo se hacía en el cliente (ver `docs/correciones
20-09.md`, citado en varios comentarios). §7 documenta los puntos donde esa regla todavía no se aplica
del todo.

## 4. Flujo de datos

### 4.1 Arranque y autenticación

1. `main.jsx` monta `<App />`, que envuelve todo en `AuthProvider → RoleProvider → DataProvider →
   <Routes>` (`App.jsx:59-274`).
2. `AuthContext` lee `localStorage["psr_aerobics_user_session_v2"]` de forma síncrona al montar
   (`AuthContext.jsx:18-25`) — si hay una sesión guardada, `isAuthenticated` es `true` desde el primer
   render, sin esperar ningún fetch.
3. `login(identifier, password)` llama `authService.login`, guarda `token`/`refreshToken`/`expiresIn`
   en `localStorage` y arma el objeto `user` que consume el resto de la app (`AuthContext.jsx:58-92`).
4. `ProtectedRoute` (`components/auth/ProtectedRoute.jsx`) es el único punto que decide si una ruta se
   renderiza: sin sesión, redirige a `/login`; con sesión pero sin el rol requerido (`allowedRoles`),
   muestra una pantalla de "Acceso Restringido" en vez de redirigir (para no perder el contexto de qué
   se intentó abrir).
5. Con la sesión resuelta, `RoleContext` decide el **rol activo** cuando la cuenta tiene más de uno
   (§5.2), y `RoleRedirect` (`App.jsx:46-57`) manda a `HOME_BY_ROLE[activeRole]`.

### 4.2 Carga de datos de personal (`DataContext`)

`DataProvider` es el único lugar que pide `people`, `queue` y `sessions` — lo hace una vez que hay
sesión **y** el usuario tiene algún rol de `STAFF_ROLES` (`ADMIN`/`RECEPTIONIST`/`TEACHER`); a un
`MEMBER` o a la cuenta técnica `PSR_TERMINAL` nunca les pide nada, porque esos tres endpoints le
responderían 403 (`DataContext.jsx:32, 40-48`). Las pantallas de `pages/member/*` y `pages/lobby/*` no
usan `DataContext` en absoluto: piden directamente por `services/` lo puntual que necesitan (el perfil
propio, la tarifa vigente, el propio check-in).

```
login / recarga de página
        │
        ▼
DataProvider.refreshData()  ──▶  GET /api/personas
                             ──▶  GET /api/cola/admin
                             ──▶  GET /api/sesiones?desde=…&hasta=…   (rango: hoy ±7 días)
        │
        ▼
setPeople / setQueue / setSessions
        │
        ▼
Cualquier página que llama useMockData() re-renderiza con los datos nuevos
```

Sobre esa base, un WebSocket STOMP (`websocketService.js`) se suscribe a `/topic/cola/admin`
(`DataContext.jsx:144-178`) — pero el mensaje que llega **no es la cola**, es solo la señal
`{sessionId, type}` de que algo cambió; el cliente reacciona releyendo por REST (`refreshQueue()`, y si
`type === "SESSION_CHANGED"` también `refreshSessions()`). Es decir: el WebSocket dispara refetches, no
transporta estado. Como red de seguridad adicional, `ReceptionCheckinPage` también hace polling cada 30s
mientras la pestaña está visible (`ReceptionCheckinPage.jsx:134-144`), por si el WebSocket no llega a
conectar.

### 4.3 Ciclo de una acción de escritura (patrón repetido en toda la app)

Todas las mutaciones (cobrar, confirmar asistencia, crear sesión, cancelar horario…) siguen la misma
forma:

```
1. Página arma el payload a partir de su propio estado (inputs controlados)
2. Llama a un método de services/*.js  → apiClient.post/put/patch/delete
3. apiClient adjunta el Bearer token y hace fetch()
4. Si el backend responde 401 y hay refresh habilitado → renueva el token y reintenta UNA vez
   (apiClient.js:125-187); si el refresh también falla, dispara "psr:unauthorized" → AuthContext
   escucha ese evento global y limpia la sesión (AuthContext.jsx:45-49)
5. Éxito → la página vuelve a pedir los datos frescos (refreshQueue/refreshSessions/etc.),
   NUNCA calcula el nuevo estado localmente a partir de la respuesta
6. Error → se captura en la página, se guarda en un `actionError` de estado y se muestra en pantalla
```

El punto 5 es una decisión explícita y repetida en comentarios (`ReceptionCheckinPage.jsx:190-196`,
`MemberHomePage.jsx:118-121`): versiones anteriores actualizaban `balance`/`amountCharged` a mano tras
cada acción, y ese cálculo optimista se desincronizaba del real (ver §7 para los lugares donde todavía
queda cálculo de negocio en el cliente antes de ese refetch).

### 4.4 Ejemplo end-to-end: cobrar en recepción

```
QueueRow (fila de una persona en la cola)
   │  persona escribe un monto, elige método de pago, pulsa "Cobrar"
   ▼
ReceptionCheckinPage.handleCharge(checkinId, amount, mode, methodId)
   │  1. calcula amountApplied / remainder / extraDays  (ver §7.1 — esto es negocio en el cliente)
   │  2. POST /api/movimientos          { personId, checkinId, paymentMethodId, amountReceived, amountApplied, note }
   │  3. si sobra vuelto y el modo no es "pending" → POST /api/movimientos/{id}/vuelto
   ▼
refreshQueue()  →  GET /api/cola/admin
   │
   ▼
DataContext.setQueue(...)  →  QueueRow vuelve a renderizar con accountStatus/amountDue/balance
                                YA CALCULADOS por el servidor, no por el paso 1
```

### 4.5 Sesión que expira / se revoca

```
Cualquier fetch → 401
   │
   ├─ REFRESH_ENABLED=true y hay refreshToken   → POST /api/auth/refresh → reintenta la petición original
   │                                                 (una sola renovación en vuelo para N peticiones
   │                                                 simultáneas, apiClient.js:36-42)
   │
   └─ si no hay refresh, o el refresh también falla
         → window.dispatchEvent("psr:unauthorized")
         → AuthContext.setUser(null) → limpia localStorage, cancela el timer de renovación
         → ProtectedRoute ya no ve isAuthenticated → redirige a /login
```

`REFRESH_ENABLED` es una bandera de entorno (`VITE_AUTH_REFRESH_ENABLED`) apagada por defecto: mientras
el backend no emita `refreshToken`, el comportamiento se reduce a "cualquier 401 expulsa al login", que
es el mismo de siempre.

## 5. Estado global (`context/`)

| Contexto | Qué guarda | Qué NO hace |
|---|---|---|
| `AuthContext` | `user` (id, roles, nombre), token en `localStorage`, ciclo login/logout/refresh | no decide autorización por pantalla — eso es `ProtectedRoute` |
| `RoleContext` | `activeRole` — cuál de los roles de la cuenta está "puesto" ahora | **no es un mecanismo de seguridad**: el propio archivo lo aclara en su cabecera (`RoleContext.jsx:2-6`) — es solo qué sidebar/home se muestra; el backend vuelve a validar contra `user.roles` completo en cada request |
| `DataContext` | `people`, `queue`, `sessions` (solo para `STAFF_ROLES`) + WebSocket de cola | no transforma esos datos: los expone tal cual llegan del backend |

`RoleContext` merece una mención aparte porque hace algo más sutil que guardar un valor: persiste, por
`userId`, tanto el rol elegido como el conjunto de roles que la cuenta tenía la última vez
(`readKnownRoles`/`writeKnownRoles`, `RoleContext.jsx:34-51`), para poder distinguir "sigue siendo
válido el rol recordado" de "le agregaron un rol nuevo desde la última vez" y forzar a elegir de nuevo
en ese segundo caso. Es una pieza de estado de UX con más lógica de la que aparenta un simple
`localStorage.setItem`, pero sigue siendo puramente de presentación — no autoriza nada.

## 6. Capa de servicios (`services/`)

Es, con diferencia, la parte más disciplinada del proyecto. Cada archivo (`peopleService.js`,
`queueService.js`, `paymentsService.js`, etc.) es una lista plana de funciones `async` que arman la URL
y el body y llaman a `apiClient`. No hay transformación de datos, no hay `if` de negocio, no hay
acumulación de estado. Los comentarios de cada método documentan el contrato real del endpoint (qué
campos son obligatorios, qué status devuelve, qué rol lo puede llamar) — en la práctica funcionan como
la documentación de la API que consume el frontend.

`apiClient.js` es la única pieza con lógica propia, y es infraestructura pura, no negocio: adjuntar el
token, parsear el `content-type`, coordinar una sola renovación de token en vuelo aunque cinco
peticiones fallen a la vez (`apiClient.js:36-73`), y programar la renovación proactiva por reloj antes
de que el token venza (`scheduleProactiveRefresh`, `apiClient.js:87-105`).

## 7. Casos donde el frontend asume responsabilidades que no le corresponden

La mayoría del código sigue de forma consistente la regla de "el servidor decide, el frontend muestra"
(§3). Los casos siguientes son las excepciones reales — algunos ya señalados como provisionales por
quienes escribieron el código, otros no.

### 7.1 Orquestación de un cobro en múltiples llamadas, con cálculo de negocio en el cliente

`ReceptionCheckinPage.handleCharge` (`pages/reception/ReceptionCheckinPage.jsx:221-268`) calcula, antes
de llamar al backend:

```js
const due = getAmountDue(item);
const extraDays = mode === "advance" && dailyRate > 0
  ? Math.floor(Math.max(0, amount - due) / dailyRate)
  : 0;
const amountApplied = Math.min(amount, due + extraDays * dailyRate);
const remainder = amount - amountApplied;
```

y además arma el texto de negocio que queda guardado como `note` del movimiento contable (líneas
240-249: "Cobro de entrada + N sesión(es) por adelantado", "Pago parcial en recepción", etc.). Con ese
resultado hace **dos peticiones separadas** para una sola operación de negocio: `POST
/api/movimientos` y, si sobra vuelto, `POST /api/movimientos/{id}/vuelto`. Si la segunda falla después
de que la primera ya se aplicó, la cuenta queda con el cobro registrado pero el vuelto sin entregar, sin
ninguna transacción que lo cubra — el frontend no tiene forma de deshacer el primer paso.

Esto no es un descuido aislado: `QueueRow.jsx:78-81` recalcula **la misma fórmula** (`extraDays`,
`appliedIfAdvance`, `changeIfAdvance`) para poder mostrar en el botón cuánto se va a abonar/devolver
antes de que la persona confirme. Es decir, la regla "cuántos días completos cubre el excedente"
vive duplicada en dos archivos que no se importan entre sí — si la regla cambia (por ejemplo, tarifas
distintas por tipo de sesión), hay que recordar tocar los dos lugares y que no queden inconsistentes
entre lo que el botón promete y lo que `handleCharge` termina enviando.

**Por qué importa:** calcular cuánto se le cobra a alguien y en qué se reparte su dinero es lo más
"de negocio" que puede existir en una app de gestión de pagos. Que dos peticiones HTTP consecutivas e
independientes representen una sola operación contable es un riesgo de consistencia real (falla
parcial), no solo una cuestión de estilo.

### 7.2 Reparto FIFO de vueltos pendientes hecho en el cliente

`findPendingMovements`, `handleDeliverPending` y `handleApplyPending`
(`ReceptionCheckinPage.jsx:311-379`) resuelven algo que el backend expone solo *por movimiento*: "el
vuelto pendiente de esta persona" en realidad puede estar repartido en varios movimientos de cobro. El
frontend:

1. Pide *todos* los vueltos pendientes del sistema (`GET /api/movimientos/pendientes/vueltos`).
2. Los filtra a mano por `personId` (o por nombre normalizado si no hay `personId`, línea 314-317 —
   comparar por nombre es en sí mismo fragil: dos personas homónimas colisionarían).
3. Los ordena del más antiguo al más nuevo (FIFO) y llama `deliverChange`/`payChangeAsCredit`
   **movimiento por movimiento, en un loop secuencial de `await`**.

El propio comentario del código lo señala sin ambigüedad (`ReceptionCheckinPage.jsx:302-308`):

> "PROVISIONAL: es orquestación que debería vivir en el servidor (una acción por persona); pedido en
> pendientes-back-20-09-seguimiento.md."

Es decir, ya está identificado como deuda técnica pendiente de que el backend exponga un endpoint por
persona (como ya existe para el reembolso general, `POST /api/personas/{id}/reembolso`, que
explícitamente reemplazó un patrón idéntico — ver el comentario en `paymentsService.js:69-71`). Hasta
que eso exista, cualquier fallo a mitad del loop deja el reparto a medias sin transacción que lo
respalde, igual que en §7.1.

### 7.3 Un número mágico de negocio duplicado en cuatro archivos

La edad mínima para registrarse (18 años, regla real: `RegisterService.MINIMUM_AGE` en el backend) se
pasa como `minAge={18}` explícito, hardcodeado, en:

- `features/reception/components/PersonFormModal.jsx:178`
- `pages/public/SelfRegistrationPage.jsx:237`
- `pages/lobby/LobbyRegistrationPage.jsx:280`
- `pages/member/MemberRegistrationPage.jsx:236`

`components/forms/BirthDateField.jsx:36` ya trae `minAge = 18` como valor por defecto del propio
componente — con lo cual los cuatro sitios de arriba son redundantes con ese default, pero cada uno lo
repite "por si acaso" (el comentario en cada uno lo explica: "minAge explícito... el backend rechaza
menores de edad"). El número 18 termina escrito cinco veces (cuatro llamadas + un default) para una
sola regla de negocio. Si el backend cambiara `MINIMUM_AGE`, hay cinco lugares que actualizar a mano y
ningún mecanismo que avise si alguno queda desincronizado — el peor caso no es un error visible, es que
el selector de fecha deje construir (o bloquee) una fecha que el backend ya no valida igual.

### 7.4 Autorización de rutas duplicada en el cliente

`ProtectedRoute` (`components/auth/ProtectedRoute.jsx:14-16`) decide qué se renderiza comparando
`allowedRoles` contra `user.roles`. Esto **no es un bug** — el backend vuelve a validar cada endpoint
por su cuenta y el propio `RoleContext.jsx` lo deja explícito en su cabecera ("si alguien manipula el
rol activo desde la consola, lo único que logra es un sidebar raro"). Pero sigue siendo una
responsabilidad de negocio (qué rol puede hacer qué) que vive **repetida** en dos sitios que no se
generan el uno del otro:

- El mapa de permisos real, expresado como `allowedRoles={[...]}` en cada `<Route>` de `App.jsx`
  (25 rutas, cada una con su propia lista escrita a mano).
- El mapa de navegación (qué ve cada rol en el sidebar), `NAV_BY_ROLE` en `constants/roles.js:69-109`.

Nada obliga a que ambos coincidan. De hecho ya hay una asimetría documentada a propósito: `TEACHER` ve
en el sidebar el link a `/reception/settings` (lectura de tarifa/config de cola) pero **no** a
`/reception/reports` (comentario en `constants/roles.js:97`), y esa distinción está codificada dos
veces — una en `NAV_BY_ROLE.TEACHER` (qué opciones aparecen) y otra en el `allowedRoles` de la ruta
`/reception/reports` en `App.jsx:174-181` (quién puede realmente entrar). Es una duplicación consciente
y hoy consistente, pero es exactamente el tipo de lugar donde un cambio de permisos futuro puede
actualizar un mapa y olvidar el otro, dejando un link visible a una ruta que redirige a "Acceso
Restringido", o (el caso peor) una ruta alcanzable que no aparece en ningún sidebar.

### 7.5 Validaciones de negocio "front-only" que deben mantenerse sincronizadas a mano

`utils/validation.js` documenta explícitamente en su propia cabecera que sus reglas (teléfono ≥10
dígitos, documento ≥6 dígitos) son "front-only" y "no reemplazan ninguna validación real del backend".
Es una decisión de producto razonable (feedback inmediato sin esperar un 400), pero el archivo mismo
señala el riesgo: la regla real del teléfono de contacto de emergencia es un regex distinto
(`^\+?[0-9 ]{7,20}$`, `validation.js:27`) que **a propósito** no reusa `MIN_PHONE_DIGITS` porque el
backend acepta números (ej. fijos de 7 dígitos) que la regla de 10 dígitos rechazaría. Mantener dos
reglas de validación de teléfono deliberadamente distintas, sin ningún test que las contraste contra el
backend real, es frágil: nada avisa si el backend cambia su regex y el frontend se queda bloqueando (o
dejando pasar) casos que ya no corresponden.

### 7.6 Documento legal (PDF) ensamblado y redactado enteramente en el cliente

`utils/personalDataPdf.js` no es solo formato de presentación: además arma el texto de pie de página
citando la norma legal aplicable ("Ley 1581 de 2012, artículo 8, literal a",
`personalDataPdf.js:18-20`) directamente en código de cliente. Es un caso menor comparado con los
anteriores — no hay riesgo de inconsistencia de datos, el documento solo lee lo que ya llegó de la API
— pero es una decisión de cumplimiento normativo (qué numeral se cita, qué secciones son obligatorias)
tomada y mantenida en un archivo de utilidades de UI, sin ningún lugar único que la relacione con la
política real (`constants/policyExcerpts.js`, que sí centraliza el resto de los textos legales de
consentimiento — ver la nota de abajo). Si la política cambia de numeral, este archivo no se actualiza
solo.

### 7.7 Nota al margen: un contraejemplo que vale la pena imitar

No todo el proyecto tiene este problema — vale la pena nombrar el patrón contrario porque es el que
debería generalizarse. `constants/policyExcerpts.js` centraliza **una sola vez** cada cita legal de la
política de tratamiento de datos, y la consumen tres pantallas distintas (`PrivacyPolicyPage`,
`RegistrationConsent`, `MissingConsentPanel`) sin que el texto se repita. `features/sessions/weekUtils.js`
hace lo mismo con las fechas: su cabecera dice explícitamente "no deciden nada del negocio... eso lo
dice el servidor". Son la prueba de que el equipo conoce el patrón correcto — los casos de §7.1 a §7.5
son lugares donde, por falta de un endpoint de backend equivalente (§7.1, §7.2) o por decisión
puntual sin revisarla después (§7.3, §7.5), todavía no se aplicó.

## 8. Resumen para quien vaya a tocar este código

- Antes de calcular algo con dinero, fechas de negocio o permisos **en una pantalla**, revisar primero
  si el campo ya viene calculado en la respuesta del backend (`accountStatus`, `amountDue`,
  `refundable`, `canClose`, `status` de sesión). Si existe, usarlo tal cual — es la convención dominante
  de todo el proyecto.
- Si hay que tocar el cobro/reembolso/vuelto de recepción (§7.1, §7.2), el problema no es solo de
  estilo: cualquier cambio ahí debería idealmente ir acompañado de mover esa orquestación al backend en
  un solo endpoint, tal como ya se hizo con `POST /api/personas/{id}/reembolso`.
- Si se cambia la edad mínima de registro o alguna otra regla repetida como constante (§7.3), buscar
  todas las ocurrencias antes de tocar solo una — no hay un único punto de verdad todavía.
- `NAV_BY_ROLE` (qué se muestra) y `allowedRoles` de cada ruta en `App.jsx` (qué se permite) son dos
  fuentes separadas (§7.4): un cambio de permisos por rol se hace en los dos sitios.
