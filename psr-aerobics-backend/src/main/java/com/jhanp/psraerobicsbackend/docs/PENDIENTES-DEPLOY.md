# Pendientes para el deploy

Lo que hay que hacer **antes** de que esto salga a internet, y lo que queda para la v2.

Contexto: la v1 sale en ~5 días, con presentación al cliente en 3–4. El alcance inicial es
**solo check-in**; el registro es abierto y el techo de un usuario público es el rol MIEMBRO.

No confundir con `PENDIENTES-V2.md` (funcionalidad aplazada) ni con `AUDITORIA-BACKEND.md` (el mapa
del backend y sus defectos). Aquí solo va lo que depende del **entorno** o de la **exposición
pública**.

---

## 1. Bloqueantes — sin esto no se sale

### 1.1 Apagar el seed de prueba 🔴

`app.seed.enabled=${SEED_ENABLED:true}` — **por defecto está encendido**. Si se despliega sin tocar
nada, se siembran 21 cuentas cuya contraseña es su número de documento (1111, 2222, 3333, 4444…) y
cinco horarios de prueba.

```bash
SEED_ENABLED=false
SEED_HISTORICAL_ENABLED=false   # ya es el valor por defecto, se pone explícito
```

Ver `AUDITORIA-BACKEND.md` §11 para las opciones de limpieza y su costo.

### 1.2 Sacar las credenciales de admin del código 🔴

`ProductionAccountsSeeder` crea el admin y las cinco terminales **con las contraseñas escritas en el
fuente**, y corre siempre, sin bandera. Cualquiera que vea el repositorio tiene ADMIN.

Mínimo: pasarlas a variables de entorno y que el seeder **no cree nada si no están puestas**. Y
cambiar la contraseña del admin en la base ya desplegada, si ya se desplegó.

### 1.3 CORS al dominio real del front

```bash
CORS_ALLOWED_ORIGINS=https://app.tudominio.com
```

Con esquema, **sin barra final**, y el dominio del **front**, no el del backend. Varios separados por
comas si hace falta.

> ⚠️ **Cuidado:** el código usa `setAllowedOriginPatterns`, no `setAllowedOrigins`. Con `allowedOrigins`,
> poner `*` junto a `allowCredentials(true)` falla ruidosamente y obliga a arreglarlo; con
> `allowedOriginPatterns`, **`*` funciona y deja la API abierta a cualquier origen sin dar ningún
> síntoma**. "Se me olvidó cambiar CORS" no se nota. Verificarlo a mano después del deploy.

> Y recordar que **CORS no protege la API**: protege al usuario en su navegador. `curl` y Postman lo
> ignoran. Lo que protege la API es el JWT.

La misma variable la usan `SecurityConfig` (HTTP) y `WebSocketConfig` (handshake STOMP), a propósito:
se cambia en un solo sitio.

### 1.4 Fijar la zona horaria 🔴

No hay ninguna zona fijada: Java usa `ZoneId.systemDefault()` y el SQL usa `CURRENT_DATE`/`now()`.
En local coinciden; en un servidor en UTC **el día cambia a las 7 de la tarde hora de Colombia**, y
con clases de 6–9 pm eso cae justo en medio de la operación.

```bash
TZ=America/Bogota
JAVA_TOOL_OPTIONS="-Duser.timezone=America/Bogota"
```

Y comprobar en el servidor de base: `SHOW timezone;` → debe decir `America/Bogota`.

### 1.5 HTTPS obligatorio

El JWT viaja en la cabecera `Authorization`. Sin TLS, cualquiera en la misma red lo lee y suplanta la
sesión. Si va detrás de un proxy, añadir:

```properties
server.forward-headers-strategy=framework
```

### 1.6 Cerrar Swagger

`/swagger-ui/**` y `/v3/api-docs/**` están en `permitAll()`: con la app en internet, **toda la API
queda documentada y navegable públicamente**, incluidos los endpoints de administración. Es un mapa
gratis para cualquiera.

```properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

(O dejarlo accesible solo desde la red interna / detrás de auth básica del proxy.)

### 1.7 Bajar el ruido de los logs

```properties
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=INFO
logging.level.org.flywaydb=INFO
```

Hoy están en DEBUG: llenan el disco y dejan las consultas (con sus valores) en el log.

---

## 2. Variables de entorno — checklist completa

| Variable | Valor en producción | Si falta |
|---|---|---|
| `POSTGRES_URL` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | los reales | no arranca |
| `JWT_SECRET` | **Base64, ≥32 bytes**, distinto al de desarrollo | no arranca (con mensaje claro) |
| `SEED_ENABLED` | `false` | (ya es el defecto desde el 28-09-2026) |
| `SEED_HISTORICAL_ENABLED` | `false` | (ya es el defecto) |
| `SEED_PRODUCTION_ENABLED` | `true` | (ya es el defecto) sin él no hay admin, terminales ni horarios |
| `ADMIN_EMAIL` | el real | queda `adminpsraerobicos@gmail.com` |
| `ADMIN_PASSWORD` | uno nuevo | 🔴 queda `adminrootpsraerobicos`, que está en el repositorio |
| `CORS_ALLOWED_ORIGINS` | dominio del front | 🔴 quedan los localhost de Vite |
| `FRONTEND_URL` | dominio del front | los enlaces de los correos apuntan a `localhost:5173` |
| `EMAIL_PROVIDER` | `brevo` | usa SMTP de Mailtrap (sandbox) |
| `BREVO_API_KEY` | la real | **no arranca** si `EMAIL_PROVIDER=brevo` |
| `EMAIL_FROM_ADDRESS` | remitente **verificado en Brevo** | Brevo rechaza con 400 |
| `EMAIL_FROM_NAME` | `PSR Aerobicos` | |
| `PRIVACY_POLICY_VERSION` | `1.0` | |
| `TZ` | `America/Bogota` | 🔴 ver 1.4 |

> `JWT_SECRET` se genera con:
> `[Convert]::ToBase64String((New-Object byte[] 64))` en PowerShell.

### ⚠️ El `.env` de la raíz NO viaja al servidor

En desarrollo esas variables salen del `.env` del proyecto, pero **no las lee ni Spring Boot ni el
`.jar`**: las inyecta el plugin **EnvFile** (`net.ashald.envfile`) de IntelliJ, configurado sobre la
run configuration `PsrAerobicsBackendApplication`. No hay ninguna dependencia dotenv en `pom.xml`.

Consecuencias:

- **Arrancando desde el botón Run del IDE**: cambiar el `.env` y reiniciar basta. No hay que
  recompilar ni tocar código.
- **`java -jar`, Docker, Render, Railway, systemd**: el `.env` se ignora por completo. Las variables
  tienen que ser variables de entorno de verdad del proceso, o el panel de secretos del proveedor.
  Copiar el `.env` al servidor no hace nada por sí solo.
- **Los tests**: la run configuration de JUnit **no** tiene la extensión EnvFile, y por eso
  `PsrAerobicsBackendApplicationTests` falla con `Could not resolve placeholder 'JWT_SECRET'` en
  cualquier máquina.
- **`.idea/workspace.xml` está en `.gitignore`**, así que esa configuración del plugin es local: en
  otra máquina hay que volver a apuntarla al `.env` a mano.

---

## 3. Rate limiting — qué hace falta de verdad

**El backend no tiene ninguno.** No hay dependencia (ni bucket4j ni resilience4j) ni filtro propio.
Tres rutas quedan expuestas y **no son igual de graves**:

| Ruta | Qué pasa si la martillean | Gravedad |
|---|---|---|
| `POST /api/auth/forgot-password` | **cada petición manda un correo** → quema cuota de Brevo | 🔴 la única que cuesta dinero |
| `POST /api/auth/login` | fuerza bruta de contraseñas | ⚠️ agravado: la contraseña inicial es el documento |
| `POST /api/personas/autoregistro` | llena la tabla + manda correo | ⚠️ pero es la funcionalidad pedida |

### Lo de login merece una frase aparte

La contraseña inicial **es el número de documento**, y una cédula no es un secreto. Quien conozca la
cédula de alguien que nunca cambió su contraseña, entra como esa persona. **Un rate limit no arregla
eso** — lo arregla forzar el cambio en el primer inicio de sesión, que está en la sección v2. El rate
limit solo encarece el barrido masivo.

### Decisión para la v1

**Cloudflare delante cubre el 80% sin tocar código:** reglas de rate limiting por IP sobre esas tres
rutas, Bot Fight Mode, y Turnstile en el formulario de recuperación. Es lo correcto con el plazo que
hay. Dos trampas que conviene no pasar por alto:

1. **Si el origen sigue accesible por su IP directa, saltarse Cloudflare es trivial.** Hay que cerrar
   el origen a los rangos de Cloudflare (firewall) o usar un Tunnel. Sin esto, todo lo demás es
   decorativo.
2. **El plan free trae una sola regla de rate limiting**: si se usa una sola, que sea sobre
   `forgot-password`, que es la que cuesta dinero.

**El frontend no puede hacer rate limiting** — un cliente no se limita a sí mismo. Lo único que aporta
es debounce y captcha, que son UX, no defensa.

**Lo mínimo que sí valdría la pena en el backend** (≈30 líneas, sin dependencias nuevas): un throttle
en memoria por correo+IP **solo** en `forgot-password`, porque es el único que se traduce en factura y
porque una regla de Cloudflare puede caerse o estar mal puesta sin que nadie se entere. Si no da
tiempo, se va a la v2 — pero entonces conviene vigilar el consumo de Brevo la primera semana.

---

## 4. SQL injection — verificado, no hace falta nada

Revisado consulta por consulta el 23-sep. **No hay superficie de inyección**:

- Las 4 consultas nativas usan parámetros nombrados (`:term`, `:roleNames`). El término va dentro de
  `'%' || :term || '%'`: **la concatenación la hace Postgres con el valor ya bindeado**, no Java
  construyendo la cadena.
- Todos los `CALL` van con placeholders (`jdbcTemplate.update("CALL sp_x(?, ?)", a, b)`), incluido
  `set_config('app.usuario_id', ?, true)`. `DashboardRepository` igual.
- Búsqueda explícita de concatenación de SQL: **cero resultados**. Las coincidencias de `" + var + "`
  son todas mensajes de error.
- Los procedimientos plpgsql reciben parámetros **tipados** y no usan `EXECUTE` dinámico en ningún
  sitio.

**Riesgo residual:** un `%` o `_` en el buscador se interpreta como comodín — patrón más amplio,
impacto nulo. No es inyección.

**El frontend no tiene nada que hacer aquí.** Lo que sí es suyo es **XSS**: el backend **no sanitiza
HTML** en los campos de texto libre (nombres, nota del cobro, observaciones de salud). React escapa
por defecto, así que solo importa si en alguna pantalla se usa `dangerouslySetInnerHTML`.

---

## 5. Datos reales pendientes de confirmar con el cliente

**No tocar hasta confirmación.** Hoy la base arranca con valores de prueba:

| Dato | Valor actual | Valor real (por confirmar) |
|---|---|---|
| Tarifa | `4000` (`V1__INITIAL_DDL.sql`) | **4.000** — coincide, confirmar |
| Horarios | L–V 18:00–19:00 y 19:00–20:00 (`TestDataSeeder`) | **lunes a jueves, 6 o 7 pm → 9 pm** |

Dos notas sobre esto:

- Los horarios de `TestDataSeeder` **desaparecen solos al poner `SEED_ENABLED=false`**: no hay que
  borrarlos, solo no sembrarlos. Los reales se crean desde la app (`POST /api/horarios`), que además
  genera sus sesiones al instante.
- La tarifa sí vive en `V1` y se aplica siempre. Si el valor real es 4.000, no hay nada que hacer; si
  cambia, se ajusta desde `PUT /api/tarifa` (queda auditado) y no tocando la migración.
- Una clase de 6 a 9 pm es una franja de **tres horas**, no de una. Conviene confirmar si es una sola
  sesión larga o varias seguidas, porque de eso depende cómo se configuran los horarios y a qué
  sesión entra quien llega a las 8 (ver `fn_obtener_o_crear_sesion_del_momento`).

---

## 6. Verificación después del primer arranque

En orden, con la base ya migrada:

1. **Flyway aplica las 5 migraciones sin error** (1 versionada + 4 repetibles). Si algo falla, empezar
   por `R__FUNCTIONS.sql`, que es el que más cambió.
2. **La lista larga de `PENDIENTES-V2.md` §2** — el SQL de cuatro rondas nunca se ha ejecutado contra
   una base real.
3. **La prueba de una línea del cobro:** persona con crédito 5 marca llegada → su perfil debe decir
   **4, no 3**. Si dice 3, se están descontando sesiones de más.
4. **Check-in por las cuatro vías** (`QR`, `DOCUMENTO`, `MANUAL`, `PROPIO`), no solo QR: los fallos
   históricos de enums sobrevivían porque con QR funcionaba de casualidad.
5. **Un correo real de bienvenida llega** (Brevo, remitente verificado, cuota disponible).
6. **El WebSocket conecta desde el dominio real** — usa la misma propiedad de CORS.
7. **Un token caduca a la hora** y hay que volver a entrar: confirmar que eso es aceptable para la
   operación del día (ver v2).

---

## 7. V2 — después del lanzamiento (1–2 semanas)

Ordenado por lo que más molesta en operación real.

### 7.1 Sesión de 1 hora sin renovación — lo primero que va a doler

El JWT dura 1 h y **no hay refresh token**: a la hora, quien está en recepción tiene que volver a
entrar. La tabla `refresh_token` ya existe en `V1` (con su índice y su columna `user_agent`) y la
propiedad `jwt.route.refresh.path` está en `application.properties`, pero **no hay una sola línea de
Java que las use**.

Dos caminos: subir la expiración a 8–12 h (5 minutos de trabajo, peor si roban el token) o
implementar el refresh (2–3 h, es lo correcto). **Para la v1 conviene al menos subirla**, o la demo
de una hora larga se cae sola.

### 7.2 Forzar el cambio de contraseña en el primer inicio de sesión

Es lo que de verdad cierra el problema de "la contraseña es el documento". Una columna
`debe_cambiar_password` y un 428/flag en la respuesta del login.

### 7.3 Rate limiting en el backend

Si en la v1 se resolvió solo con Cloudflare, aquí conviene meterlo en el código (bucket4j o un filtro
propio) para no depender de que la regla del CDN esté bien puesta.

### 7.4 Autorización por destino en el WebSocket

Hoy cualquier usuario autenticado —incluido un MIEMBRO— puede suscribirse a `/topic/cola/admin`. No
filtra datos **porque el mensaje va vacío**, pero la defensa es esa, no que el topic esté cerrado: el
día que alguien meta datos en el evento, el agujero se abre solo. Ver `AUDITORIA-BACKEND.md` §8.

### 7.5 Tests

`src/test` está vacío. Cero cobertura automática. No es pagable antes del lanzamiento, pero es la
mayor deuda técnica del proyecto y conviene empezar por lo que toca dinero: `fn_monto_a_pagar`,
`fn_saldo_reembolsable` y el reparto FIFO de reembolsos.

### 7.6 Los dos fallos silenciosos de transacción

`SessionClosingScheduler` publica su evento sin transacción activa (se descarta sin error), y la
relectura tras la confirmación automática devuelve la fila cacheada. 30 minutos entre los dos. Ver
`AUDITORIA-BACKEND.md` §14 #3 y #4.

### 7.7 Método de pago propio para el vuelto

Cobrar por transferencia y devolver en efectivo no se puede registrar hoy. **Mientras la base se
pueda recrear de cero es barato** (una columna en `V1`); con datos reales dentro, exige migración
incremental. Ver `PENDIENTES-V2.md` §3 — conviene decidirlo **antes** de que entren datos de verdad.

### 7.8 Ampliar la auditoría

Hoy cubre 4 tablas de 6 en la práctica: editar nombre o teléfono de una persona no deja rastro, y
`ASISTENCIA` no se audita nunca porque ningún endpoint modifica `monto_cobrado`. Ya está decidido
dejarlo así para la v1 porque ampliarlo toca esquema.
