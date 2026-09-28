# Variables de entorno

Todo lo que `application.properties` espera del exterior. Copiar a un `.env` en la raíz y rellenar.

> El `.env` real está en `.gitignore`. Este documento existe para que se sepa **qué** hace falta sin
> exponer ningún valor.

---

## Base de datos

| Variable | Ejemplo | Notas |
|---|---|---|
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/psr` | |
| `POSTGRES_USER` | | |
| `POSTGRES_PASSWORD` | | |

Si en tu `.env` hay `POSTGRES_HOST` o `POSTGRES_DB`, no los lee nadie: la conexión entera sale de
`POSTGRES_URL`. Son inofensivos, pero confunden a quien venga detrás.

## JWT

| Variable | Notas |
|---|---|
| `JWT_SECRET` | Base64 de **al menos 32 bytes**. |

La app **no arranca** si es inválido o corto, y es a propósito: mejor reventar al inicio con un
mensaje claro que en el primer login con una traza de Base64 que no dice de dónde viene.

Generar en PowerShell:

```powershell
[Convert]::ToBase64String((New-Object byte[] 64))
```

## Frontend y CORS

| Variable | Por defecto | Notas |
|---|---|---|
| `FRONTEND_URL` | `http://localhost:5173` | Base de los enlaces que van **dentro de los correos** |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5174,http://localhost:5173,http://localhost:5175` | Separados por coma |

Los tres puertos por defecto en CORS no son capricho: Vite arranca en 5173 y va subiendo si el puerto
está ocupado, así que dejarlos los tres evita tener que tocar esto cada vez que cambia.

`FRONTEND_URL` tiene que apuntar al front **desplegado**: de ahí salen `/login` y
`/restablecer-password?token=…`. Si apunta a localhost en producción, los correos llegan con enlaces
que no abren.

`CORS_ALLOWED_ORIGINS` la comparten el CORS de HTTP (`SecurityConfig`) y el handshake del WebSocket
(`WebSocketConfig`), justamente para que no puedan desincronizarse. **En producción no puede quedar
en `*`.**

## Correo

| Variable | Por defecto | Notas |
|---|---|---|
| `EMAIL_PROVIDER` | `smtp` | `smtp` en local, `brevo` en producción |
| `EMAIL_FROM_ADDRESS` | `no-reply@psr-aerobicos.local` | |
| `EMAIL_FROM_NAME` | `PSR Aerobicos` | |

Solo se registra el sender correspondiente al proveedor elegido.

⚠️ **`EMAIL_PROVIDER` tiene que valer exactamente `brevo`, no `brevo-api` ni nada parecido.** El
sender se activa con `@ConditionalOnProperty(havingValue = "brevo")`, así que cualquier otra cadena lo
deja apagado, cae de vuelta a SMTP **sin avisar** y los correos dejan de salir. Ya pasó una vez y
costó un día encontrarlo: la pista es la línea `SmtpEmailSender matched (app.email.provider=smtp)` en
el log de arranque, que delata el proveedor real.

### Con `EMAIL_PROVIDER=brevo`

| Variable | Notas |
|---|---|
| `BREVO_API_KEY` | Obligatoria |

El arranque **falla** si falta, para no descubrirlo cuando el primer usuario se quede sin su correo.
El remitente de `EMAIL_FROM_ADDRESS` tiene que estar verificado en la cuenta de Brevo.

### Con `EMAIL_PROVIDER=smtp`

| Variable | Por defecto |
|---|---|
| `SMTP_HOST` | `sandbox.smtp.mailtrap.io` |
| `SMTP_PORT` | `587` |
| `SMTP_USER` | |
| `SMTP_PASSWORD` | |

## Política de tratamiento de datos

| Variable | Por defecto | Notas |
|---|---|---|
| `PRIVACY_POLICY_VERSION` | `1.0` | Versión del documento que se congela en cada consentimiento |

Cada fila de `consentimiento` guarda con qué versión de la política se aceptó, porque "aceptó" sin
decir QUÉ aceptó no prueba nada el día que el texto cambie. Está en configuración y no en una tabla
porque la decisión tomada es que la política se queda en v1 única, sin sistema de versiones.

**Solo hay que tocarla si se publica una versión nueva del documento**, y entonces hay que cambiarla
*el mismo día*: las filas nuevas quedan marcadas con lo que diga esta variable, así que dejarla atrás
hace que gente que aceptó la v2 conste como si hubiera aceptado la v1. Las filas viejas no se tocan.

## Seed de producción

| Variable | Por defecto | Notas |
|---|---|---|
| `SEED_PRODUCTION_ENABLED` | `true` | Admin de arranque, 3 terminales PSR, cuentas Invitado y Menor de edad, y la agenda (lunes–jueves 19:00–20:00) |
| `ADMIN_EMAIL` | `adminpsraerobicos@gmail.com` | |
| `ADMIN_PASSWORD` | `adminrootpsraerobicos` | **Cámbiala en el primer login.** |

Este sí viene **encendido** por defecto, al contrario que los dos de abajo, y no es un descuido: sin
el admin no hay por dónde entrar a crear las cuentas reales, y sin horarios no hay clase a la que
llegar. Un despliegue al que se le olvide la variable arranca usable.

Las credenciales también tienen valor por defecto a propósito. Si fueran obligatorias, un despliegue
sin definirlas arrancaría **sin ningún admin** y el sistema quedaría cerrado por fuera. Una
contraseña conocida se cambia desde la aplicación; un sistema sin admin, no.

⚠️ **Cambiar `ADMIN_EMAIL` no renombra al admin que ya existe.** La siembra es idempotente por
correo: apuntarlo a otra dirección sobre una base ya sembrada crea un **segundo** admin y deja el
primero intacto, con su contraseña vieja. Para cambiar de correo de verdad hay que desactivar el
viejo a mano.

Se apaga cuando las cuentas reales ya existen, o para levantar la app contra una base ajena sin
escribir en ella.

## Seed de prueba

| Variable | Por defecto |
|---|---|
| `SEED_ENABLED` | `false` |
| `SEED_HISTORICAL_ENABLED` | `false` |

Los dos vienen apagados: sin definir nada, un despliegue no siembra ni una cuenta de prueba.

⚠️ **`SEED_ENABLED` venía en `true` hasta el 28-09-2026** y este documento avisaba de ello. Cambió a
`false`: el único motivo del `true` era que en una base recién migrada no hubiera ningún ADMIN con el
que entrar, y eso ahora lo cubre `ProductionAccountsSeeder`, que corre siempre y sin bandera. Si
despliegas contra una base donde ya corrió el seed viejo, sus cuentas siguen ahí — hay que
desactivarlas a mano.

Crea una cuenta por rol con la contraseña igual al número de documento, más la cuenta técnica
`terminal@psraerobicos.com` / `Terminal2026!` con rol `PSR_TERMINAL`.

**Encenderlo en local tiene un motivo extra:** es el único que siembra los horarios de lunes a
viernes. Sin él la agenda arranca vacía y hay que crear los horarios desde la pantalla de admin.

**Debe quedar en `false` fuera de tu máquina:** son credenciales conocidas.

### `SEED_HISTORICAL_ENABLED`

Cinco días hábiles de operación ya transcurrida: 45 personas, ~30 asistencias por sesión, cobros,
abonos por adelantado, vueltos, reembolsos, una sesión cancelada con gente colgando y un método de
pago desactivado. Sirve para revisar reportes, historiales y saldos sin fabricar los datos a mano.

**Bandera aparte de `SEED_ENABLED` a propósito**, y apagada por defecto: las cuentas de prueba se
quieren casi siempre, y ciento cincuenta asistencias inventadas solo en el entorno de pruebas.

**Depende de `SEED_ENABLED=true`**: necesita un ADMIN y un PROFESOR ya creados para poder atribuirles
cada asistencia y cada movimiento (la base lo exige, esas columnas son `NOT NULL`). Si no los
encuentra, lo dice en el log y no escribe nada.

Las fechas son relativas a hoy, así que el dato no envejece. Es idempotente: si ya se sembró, no
vuelve a hacerlo. Detalle de lo que carga, en `NOTAS-PARA-FRONTEND.md` (bloque 3, punto 4).

**Nunca en producción.**

---

## Checklist de producción

- [ ] `JWT_SECRET` distinto al de desarrollo
- [ ] `FRONTEND_URL` al dominio real
- [ ] `CORS_ALLOWED_ORIGINS` al dominio real, nunca `*`
- [ ] `EMAIL_PROVIDER=brevo` con `BREVO_API_KEY` y remitente verificado
- [ ] `SEED_ENABLED=false`
- [ ] `SEED_HISTORICAL_ENABLED=false` (o sin definir)
- [ ] `PRIVACY_POLICY_VERSION` coincide con la versión del documento realmente publicado

---

## Nota

Se intentó dejar esto como `.env.example` en la raíz —el `.gitignore` ya tiene la negación
`!.env.example` preparada— pero el guard del IDE bloquea escribir archivos con ese nombre por
tratarse de material de credenciales. Si quieres el archivo versionado en la raíz, créalo a mano
copiando las variables de este documento.
