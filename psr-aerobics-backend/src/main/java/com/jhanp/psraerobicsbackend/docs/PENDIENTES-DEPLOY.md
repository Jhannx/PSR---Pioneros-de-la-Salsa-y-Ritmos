# Pendientes para el deploy

Lo que hay que hacer **antes** de que esto salga a internet, y lo que queda para la v2.

Contexto: la v1 sale en ~5 días, con presentación al cliente en 3–4. El alcance inicial es
**solo check-in**; el registro es abierto y el techo de un usuario público es el rol MIEMBRO.

No confundir con `PENDIENTES-V2.md` (funcionalidad aplazada) ni con `AUDITORIA-BACKEND.md` (el mapa
del backend y sus defectos). Aquí solo va lo que depende del **entorno** o de la **exposición
pública**.

---

## 1. Bloqueantes — sin esto no se sale

### 1.1 Apagar el seed de prueba ✅ HECHO (28-09-2026)

`app.seed.test.enabled` pasó de `${SEED_TEST_ENABLED:true}` a `${SEED_TEST_ENABLED:false}`. Ya no hay que acordarse
de nada: sin definir la variable, no se siembra ninguna cuenta de prueba.

Ver `AUDITORIA-BACKEND.md` §11 para limpiar una base donde el seed viejo ya corrió.

### 1.2 Sacar las credenciales de admin del código ✅ HECHO (28-09-2026), con una desviación

El correo y la contraseña del admin salieron del fuente a `ADMIN_EMAIL` / `ADMIN_PASSWORD`, y el
seeder ganó su bandera (`SEED_PRODUCTION_ENABLED`). Las terminales bajaron de cinco a tres.

⚠️ **Lo que este documento pedía y NO se hizo:** "que el seeder no cree nada si no están puestas".
Las variables conservan un valor por defecto. El motivo es que la alternativa es peor: un despliegue
sin definirlas arrancaría **sin ningún admin**, y sin admin no hay por dónde entrar a crear las
cuentas reales — el sistema queda cerrado por fuera y hay que ir a la base a mano. Una contraseña
conocida se cambia desde la aplicación en el primer login; eso sí se recupera.

**Consecuencia que sigue viva:** si no se define `ADMIN_PASSWORD`, la contraseña es
`adminrootpsraerobicos`, que está en el repositorio. Definirla en el hoster, o cambiarla en el primer
login.

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

### 1.4 Fijar la zona horaria ✅ HECHO (28-09-2026)

Se fija en el código, no en una variable: la academia está en Colombia y no se va a mover.
`PsrAerobicsBackendApplication` la establece en un bloque `static`, **antes** de
`SpringApplication.run`.

```java
static { TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota")); }
```

Esto arregla las dos mitades a la vez, y la segunda no es obvia: el driver de PostgreSQL manda un
`SET TIME ZONE` con la zona por defecto de la JVM al abrir cada conexión, así que `CURRENT_DATE` y
`now()` **dentro de las funciones y los procedimientos** quedan también en hora de Colombia. Por eso
va en un `static` y no en un `@PostConstruct`: si no, el pool podría abrir conexiones con la zona
vieja antes de que corra.

El riesgo que eliminaba: en un servidor en UTC el día cambiaba a las 7 de la tarde hora de Colombia
— justo al empezar la clase.

> Sigue siendo buena idea poner `TZ=America/Bogota` en el hoster, para el sistema operativo del
> contenedor y para los logs. Pero ya no es lo que sostiene la corrección de las fechas.
>
> Y comprobar en el servidor de base: `SHOW timezone;`.

### 1.5 HTTPS obligatorio

El JWT viaja en la cabecera `Authorization`. Sin TLS, cualquiera en la misma red lo lee y suplanta la
sesión. Si va detrás de un proxy, añadir:

```properties
server.forward-headers-strategy=framework
```

### 1.6 Cerrar Swagger ✅ HECHO (28-09-2026)

Apagado por defecto en `application.properties`. No hay que poner nada en el hoster.

```properties
springdoc.api-docs.enabled=${SWAGGER_ENABLED:false}
springdoc.swagger-ui.enabled=${SWAGGER_ENABLED:false}
```

Para recuperarlo **en local**: `SWAGGER_ENABLED=true` en el `.env`.

### 1.7 Bajar el ruido de los logs ✅ HECHO (28-09-2026)

```properties
spring.jpa.show-sql=${SQL_LOG:false}
spring.jpa.properties.hibernate.format_sql=${SQL_LOG:false}
logging.level.org.hibernate.SQL=${SQL_LOG_LEVEL:INFO}
logging.level.org.flywaydb=INFO
```

No era solo el disco: en DEBUG el log guardaba las consultas **con sus valores**, o sea documentos,
correos y teléfonos de personas reales en un archivo que se rota y se envía a un agregador.

Para depurar una consulta en local: `SQL_LOG=true` y `SQL_LOG_LEVEL=DEBUG` en el `.env`.

---

## 2. Variables de entorno — checklist completa

| Variable | Valor en producción | Si falta |
|---|---|---|
| `POSTGRES_URL` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | los reales | no arranca |
| `JWT_SECRET` | **Base64, ≥32 bytes**, distinto al de desarrollo | no arranca (con mensaje claro) |
| `SEED_TEST_ENABLED` | `false` | (ya es el defecto desde el 28-09-2026) |
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
| `TZ` | `America/Bogota` | recomendable, pero ya no bloquea: la zona se fija en el codigo (ver 1.4) |
| `SWAGGER_ENABLED` | no ponerla | apagado por defecto; `true` solo en local |
| `SQL_LOG` / `SQL_LOG_LEVEL` | no ponerlas | logs de SQL apagados por defecto; encender solo en local |

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

## 5. Datos reales ✅ CONFIRMADOS Y APLICADOS (28-09-2026)

Esta sección decía "pendientes de confirmar" y listaba los horarios de prueba. Ya no: el cliente los
confirmó y están sembrados.

| Dato | Valor real | Dónde vive |
|---|---|---|
| Tarifa | **$4.000** | `V1__INITIAL_DDL.sql` — coincidía, no hubo que cambiar nada |
| Horarios | **lunes a jueves, 19:00–20:00** | `ProductionAccountsSeeder.seedSchedules()` |
| Margen de ingreso | **45 min** (check-in abre 18:15) | `V1__INITIAL_DDL.sql`, fila de `configuracion_cola` |
| Personas por turno | 1 | `V1__INITIAL_DDL.sql` |
| Visibilidad de pagos | 1 min | `V1__INITIAL_DDL.sql` |

Notas:

- La duda de "¿6 a 9 pm es una franja de tres horas o varias?" quedó resuelta: es **una sola sesión
  de una hora**, de 7 a 8. Viernes no hay clase.
- Los horarios **ya no viven en `TestDataSeeder`**. Estaban ahí y eran el único dato de operación
  real atado a una bandera que en producción debe estar apagada; se movieron al seeder de
  producción. Apagar `SEED_TEST_ENABLED` ya no deja la agenda vacía.
- `seedSchedules()` **siembra, no sincroniza**: crea el horario que falta y no toca los existentes,
  porque cambiar la agenda es del admin desde `POST /api/horarios`. Cambiar la hora en el código
  sobre una base ya sembrada **añadiría un segundo horario**, no movería el primero.
- La tarifa se ajusta desde `PUT /api/tarifa` (queda auditado), no tocando la migración.

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
