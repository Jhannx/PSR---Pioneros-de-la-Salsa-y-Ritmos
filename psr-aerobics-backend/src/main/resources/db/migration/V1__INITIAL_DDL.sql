-- Tipos de datos (ENUMS) y tablas

-- Tipos de datos y extensiones
-- PSR Aeróbicos — Extensiones y tipos (enums)

-- pgcrypto ya no la usa nadie: los tokens de contraseña se generan en Java (PasswordTokenService), que
-- necesita el valor en claro para meterlo en el correo. Se deja instalada porque gen_random_uuid() y
-- las funciones de digest son útiles y la extensión no cuesta nada.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- unaccent: quita las tildes al comparar, para que los buscadores encuentren igual.
--
-- Sin esto, escribir "perez" no encuentra a "Pérez" y "munoz" no encuentra a "Muñoz". Con nombres
-- colombianos eso no es un caso raro: es lo que la gente teclea siempre, porque nadie pone tildes
-- cuando busca. Lo usan las consultas de búsqueda de persona y de cuenta.
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TYPE fuente_registro_enum AS ENUM (
    'ADMIN', 'AUTOREGISTRO'
    );

CREATE TYPE tipo_documento_enum AS ENUM (
    'CC', 'TI', 'CE', 'PA'
    );

-- PROPIO: la llegada que marca la propia persona desde su pantalla de miembro.
--
-- No se llama APP ni QR a proposito. El QR de la sala es una URL que abre esa misma pantalla, asi que
-- las dos cosas entran por el mismo endpoint con el mismo token: el servidor no puede distinguirlas y
-- un nombre que afirmara cual fue estaria inventando. Lo unico que consta es que lo marco la persona
-- y no un mostrador.
--
-- Antes ese caso se guardaba como DOCUMENTO, reutilizando el valor de la terminal del lobby, y el
-- check-in de recepcion mostraba "Terminal PSR" para alguien que habia marcado su llegada desde el
-- telefono. El dato guardado era el que mentia, no la pantalla.
CREATE TYPE metodo_checkin_enum AS ENUM (
    'QR', 'DOCUMENTO', 'MANUAL', 'PROPIO'
    );

CREATE TYPE tipo_movimiento_enum AS ENUM (
    'ABONO', 'REEMBOLSO'
    );

CREATE TYPE tabla_auditable_enum AS ENUM (
    'ASISTENCIA', 'MOVIMIENTO_CUENTA', 'TARIFA', 'CONFIGURACION_COLA', 'PERSONA', 'SESION'
    );

CREATE TYPE rol_nombre_enum AS ENUM (
    'ADMIN', 'MIEMBRO', 'PROFESOR', 'RECEPCIONISTA', 'PSR_TERMINAL'
    );

-- Tablas

-- PSR Aeróbicos — Tablas

-- ROL --
CREATE TABLE rol (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre rol_nombre_enum NOT NULL UNIQUE
);

-- PERSONA --
CREATE TABLE persona (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_documento tipo_documento_enum NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NOT NULL,

    -- NULLABLE a propósito, aunque la aplicación la exija en las altas nuevas. Las personas ya
    -- registradas no tienen el dato, y un NOT NULL aquí obligaría a inventarles una fecha o a dejar
    -- la migración sin aplicar. La obligatoriedad vive en PersonRequest (@NotNull + @Past), que es
    -- donde se puede distinguir "alta nueva" de "fila histórica".
    --
    -- Para qué: gestos de la academia, saber cumpleaños. No hay ninguna regla de negocio por edad, y
    -- guardar la fecha no da por sí sola el aviso de "cumpleaños esta semana" — eso necesitaría una
    -- vista y una pantalla, y queda fuera de alcance.
    fecha_nacimiento DATE,

    -- Vive en la PERSONA y no en contacto_emergencia. Antes estaba allí, cuando el contacto era uno
    -- solo; con varios contactos deja de tener sentido: una alergia o una condición cardiaca es de
    -- quien entra a la clase, no de a quién hay que llamar. Si se quedara en el contacto habría que
    -- decidir cuál de los cinco la lleva, o repetirla en todos.
    observaciones_salud TEXT,

    fuente_registro fuente_registro_enum NOT NULL,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT now(),
    activo BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_persona_documento UNIQUE (numero_documento)
);

-- CONTACTO_EMERGENCIA --
-- Hasta cinco por persona, todos con el mismo peso: no hay contacto "principal" ni orden de
-- prioridad. Quien atiende una urgencia llama por la lista, no por un ranking que nadie mantiene.
CREATE TABLE contacto_emergencia (
    id  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- SIN UNIQUE: aquí estaba la relación 1:1. Quitarlo es lo que convierte la tabla en 1:N, y
    -- arrastra dos cosas que no son obvias:
    --   1. El tope de 5 tiene que ser un TRIGGER. Un CHECK no puede contar filas de su propia tabla.
    --      Ver trg_limitar_contactos_emergencia en R__TRIGGERS.sql.
    --   2. vista_perfil_persona ya NO puede unir contra esta tabla: con tres contactos devolvería a
    --      la misma persona tres veces, y con ella su saldo y su conteo de asistencias. Las columnas
    --      de contacto salieron de esa vista; la lista la sirve su propio endpoint.
    persona_id INTEGER NOT NULL REFERENCES persona(id),

    nombre VARCHAR(150),
    telefono VARCHAR(20),

    -- observaciones se fue a persona.observaciones_salud. No es un contacto, es la persona.

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Dos columnas y no tres, porque la tercera ya no está aquí. Un contacto sin nombre ni teléfono
    -- no es un contacto.
    CONSTRAINT chk_contacto_emergencia_no_vacio
    CHECK (nombre IS NOT NULL OR telefono IS NOT NULL)
);
-- Lo daba gratis el UNIQUE que acabamos de quitar, y sin él cada "dame los contactos de esta
-- persona" sería un recorrido secuencial de la tabla.
CREATE INDEX idx_contacto_emergencia_persona ON contacto_emergencia (persona_id);

-- USUARIO --
CREATE TABLE usuario (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_documento VARCHAR(20) UNIQUE,
    -- Sin UNIQUE aquí: lo pone uq_usuario_correo_lower, más abajo, que además ignora mayúsculas.
    correo VARCHAR(150),
    password_hash TEXT NOT NULL,
    persona_id INTEGER REFERENCES persona(id),
    activo BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_usuario_identificador CHECK (numero_documento IS NOT NULL OR correo IS NOT NULL)
);
CREATE INDEX idx_usuario_persona ON usuario (persona_id);

-- Unicidad de correo insensible a mayúsculas.
--
-- Un UNIQUE normal compara byte a byte: "Ana@x.com" y "ana@x.com" serían dos filas válidas. El día
-- que alguien se registre con mayúsculas y luego escriba su correo como lo escribe siempre, no podrá
-- entrar — y habrá dos cuentas para la misma persona.
--
-- Va en la base y no solo en Java porque el correo se escribe desde más de un sitio: UserService
-- normaliza a minúsculas, pero sp_registrar_usuario_para_persona y el seeder insertan directo. Una
-- restricción en la tabla la cumplen todos los caminos.
--
-- Los NULL no chocan entre sí: Postgres considera distintos dos NULL en un índice único, así que las
-- cuentas identificadas solo por documento siguen conviviendo sin problema.
CREATE UNIQUE INDEX uq_usuario_correo_lower ON usuario (LOWER(correo));

-- USUARIO_ROL --
-- Un usuario puede tener varios roles simultáneos, ejemplo: Un administrador que a la vez puede ser profesor
CREATE TABLE usuario_rol (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    rol_id INTEGER NOT NULL REFERENCES rol(id),
    asignado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuario_rol UNIQUE (usuario_id, rol_id)
);
CREATE INDEX idx_usuario_rol_rol ON usuario_rol (rol_id);

-- CONSENTIMIENTO --
-- Registro probatorio de autorizaciones de tratamiento de datos — Ley 1581 de 2012 y Decreto 1377
-- de 2013.
--
-- El Decreto 1377 no exige que la persona acepte: exige que el responsable PUEDA PROBAR que aceptó.
-- Una casilla marcada en el navegador no prueba nada si su estado no llega nunca al servidor — si el
-- alta falla justo después de marcarla, no queda rastro de que se marcó. Esta tabla es el rastro.
--
-- Va después de usuario porque referencia a las dos: a persona (de quién es el consentimiento) y a
-- usuario (quién lo afirmó).
CREATE TABLE consentimiento (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    persona_id INTEGER NOT NULL REFERENCES persona(id),

    -- VARCHAR con CHECK y no un tipo ENUM de Postgres, a contracorriente del resto de este archivo.
    -- La razón es concreta y está documentada en Person.java: una columna de tipo enum escrita desde
    -- Hibernate a través de un AttributeConverter se bindea como varchar, y Postgres rechaza la
    -- sentencia entera con "column X is of type Y_enum but expression is of type character varying".
    -- persona se salva de eso porque nadie la inserta por JPA — la escribe sp_registrar_persona.
    -- Esta tabla SÍ se inserta por JPA, y en cada alta, así que el enum de Postgres sería un 500
    -- garantizado. El CHECK da la misma garantía de dominio y el valor sigue en español.
    --
    -- Los cuatro valores son los cuatro numerales de la política, y las cuatro casillas del front:
    --   TRATAMIENTO_DATOS         numeral 4.1 — autorización general
    --   PARTICIPACION_VOLUNTARIA  numeral 12  — declaración de riesgo de lesión
    --   DATOS_SALUD               numeral 4.2 — dato sensible, consentimiento diferenciado y expreso
    --   DATOS_TERCEROS            numeral 5   — declaración sobre el contacto de emergencia
    tipo VARCHAR(30) NOT NULL
        CONSTRAINT chk_consentimiento_tipo CHECK (tipo IN (
            'TRATAMIENTO_DATOS', 'PARTICIPACION_VOLUNTARIA', 'DATOS_SALUD', 'DATOS_TERCEROS')),

    -- false = revocación. El artículo 8 de la Ley 1581 da derecho a revocar, y una revocación es un
    -- hecho fechado igual que la aceptación: se registra como OTRA fila, nunca borrando la anterior.
    otorgado BOOLEAN NOT NULL,

    -- Denormalizada a propósito. Hoy la política es v1 única y no hay sistema de versiones —decisión
    -- tomada, ver pendiente-implementar-sistema.md—, pero "aceptó" sin decir QUÉ aceptó no prueba
    -- nada el día que el texto cambie. Guardarlo en la fila cuesta una columna y evita tener que
    -- reconstruir después qué versión regía cada día.
    version_politica VARCHAR(10) NOT NULL,

    -- DIGITAL = casilla marcada en pantalla (autoregistro, kiosco, perfil del miembro).
    -- FISICO  = formato en papel firmado, que es como funciona el alta manual de recepción. Sin este
    --           valor, la mitad de las altas no tendrían forma de dejar constancia.
    medio VARCHAR(10) NOT NULL
        CONSTRAINT chk_consentimiento_medio CHECK (medio IN ('DIGITAL', 'FISICO')),

    fecha TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- NULL significa "no había sesión", no "lo hizo la persona": es el autoregistro, que es público y
    -- no tiene usuario autenticado a quien atribuirlo. Con valor es la cuenta que lo afirmó, que
    -- puede ser la propia persona desde su perfil o alguien del mostrador en su nombre — se
    -- distinguen comparando este id con la cuenta de la persona.
    registrado_por INTEGER REFERENCES usuario(id)
);

-- La consulta que se hace siempre es "el estado vigente de este tipo para esta persona", que es la
-- fila más reciente del par. Sin el índice, cada validación de salud o de terceros sería un recorrido
-- secuencial de una tabla que solo crece.
CREATE INDEX idx_consentimiento_persona_tipo
    ON consentimiento (persona_id, tipo, fecha DESC);

COMMENT ON TABLE consentimiento IS
    'Registro probatorio de autorizaciones de tratamiento de datos (Ley 1581/2012, Decreto 1377/2013). '
        'Solo se inserta: nunca UPDATE ni DELETE. Una prueba que se puede editar no es una prueba, y el '
        'estado vigente es siempre la fila más reciente por (persona_id, tipo).';

-- Nota sobre lo que NO lleva esta tabla: no hay ip ni user_agent.
--
-- Serían evidencia barata, pero el numeral 2.8 de la política ya publicada declara qué recoge el
-- sistema y la IP no está en esa lista. Añadir una columna que recoge un dato personal no declarado,
-- justo en la tabla que existe para demostrar cumplimiento, es la contradicción más cara posible.
-- Además, en el kiosco la IP es siempre la misma máquina y no distingue a nadie: quien identifica el
-- acto es registrado_por.

-- HORARIO --
CREATE TABLE horario (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dia_semana INTEGER NOT NULL, -- ISO: 1 = lunes ... 7 = domingo
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_horario_dia_semana CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_horario_horas CHECK (hora_fin > hora_inicio)
);
CREATE INDEX idx_horario_dia_semana ON horario (dia_semana) WHERE activo = true;

-- HORARIO_EXCEPCION --
CREATE TABLE horario_excepcion (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    horario_id INTEGER NOT NULL REFERENCES horario(id),
    fecha DATE NOT NULL,
    motivo TEXT,
    CONSTRAINT uq_horario_excepcion UNIQUE (horario_id, fecha)
);

-- SESION --
CREATE TABLE sesion (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    horario_id INTEGER REFERENCES horario(id),
    fecha DATE NOT NULL,
    hora_inicio TIMESTAMPTZ NOT NULL,
    hora_fin TIMESTAMPTZ, -- NULL mientras la clase sigue en curso
    profesor_usuario_id INTEGER REFERENCES usuario(id),
    cancelada BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT chk_sesion_horas CHECK (hora_fin IS NULL OR hora_fin > hora_inicio)
);

-- Máximo una sesión por horario por día — pero varias sesiones
-- desacopladas (horario_id NULL) sí pueden coexistir el mismo día.
CREATE UNIQUE INDEX uq_sesion_horario_fecha ON sesion (horario_id, fecha) WHERE horario_id IS NOT NULL;
CREATE INDEX idx_sesion_fecha ON sesion (fecha);

-- LLEGADA --
-- Única por (persona, sesion), no por (persona, fecha), ya que un mismo día puede tener más de una sesión válida.
CREATE TABLE llegada (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    persona_id INTEGER NOT NULL REFERENCES persona(id),
    sesion_id INTEGER NOT NULL REFERENCES sesion(id),
    hora_llegada TIMESTAMPTZ NOT NULL DEFAULT now(),
    metodo_checkin metodo_checkin_enum NOT NULL
);
-- Excluye a la persona sentinel (id 999999999): acumula llegadas de todas las personas eliminadas, así que puede repetirse en una misma sesión.
CREATE UNIQUE INDEX uq_llegada_persona_sesion ON llegada (persona_id, sesion_id) WHERE persona_id <> 999999999;
CREATE INDEX idx_llegada_persona ON llegada (persona_id);
CREATE INDEX idx_llegada_sesion ON llegada (sesion_id);

-- ASISTENCIA --
CREATE TABLE asistencia (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    llegada_id INTEGER NOT NULL UNIQUE REFERENCES llegada(id),
    -- NUMERIC(10,0): pesos colombianos enteros. El centavo no circula, así que la base rechaza un
    -- 4000.50 en vez de aceptarlo y arrastrarlo. Ver la nota sobre dinero en V1, más abajo.
    monto_cobrado NUMERIC(10,0) NOT NULL,
    marcado_por INTEGER NOT NULL REFERENCES usuario(id),
    hora_marcado TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_monto_cobrado_no_negativo CHECK (monto_cobrado >= 0)
);

-- METODO_PAGO --
CREATE TABLE metodo_pago (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT true
);

-- MOVIMIENTO_CUENTA --
CREATE TABLE movimiento_cuenta (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    persona_id INTEGER NOT NULL REFERENCES persona(id),
    llegada_id INTEGER REFERENCES llegada(id),
    monto_recibido NUMERIC(10,0) NOT NULL,
    monto_abonado NUMERIC(10,0) NOT NULL,
    metodo_pago_id INTEGER NOT NULL REFERENCES metodo_pago(id),
    tipo_movimiento tipo_movimiento_enum NOT NULL DEFAULT 'ABONO',
    movimiento_original_id INTEGER REFERENCES movimiento_cuenta(id),
    vuelto_entregado BOOLEAN NOT NULL DEFAULT false,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    nota TEXT,
    CONSTRAINT chk_montos_segun_tipo CHECK (
        (tipo_movimiento = 'ABONO' AND monto_recibido >= 0 AND monto_abonado >= 0 AND monto_abonado <= monto_recibido)
        OR
        (tipo_movimiento = 'REEMBOLSO' AND monto_recibido = 0 AND monto_abonado < 0)
    )
);
CREATE INDEX idx_movimiento_persona ON movimiento_cuenta (persona_id);
CREATE INDEX idx_movimiento_llegada ON movimiento_cuenta (llegada_id);

-- TOKEN_RECUPERACION --
-- Token de un solo uso, con caducidad, que autoriza a fijar una contraseña sin conocer la anterior.
-- Cubre los dos casos: la invitación del alta y el "olvidé mi contraseña". El mecanismo es idéntico;
-- lo único que cambia es cuánto dura y qué correo sale, y eso lo decide Java.
CREATE TABLE token_recuperacion (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- UNIQUE, no un índice a secas: pedir un token nuevo invalida el anterior. Sin esto, una segunda
    -- solicitud dejaría dos enlaces vivos a la vez.
    usuario_id INTEGER NOT NULL UNIQUE REFERENCES usuario(id),

    -- El HASH SHA-256 del token, nunca el token. El valor original solo existe dentro del correo: ni
    -- la base de datos ni los logs lo ven. Al recibir uno se hashea y se busca por esta columna.
    -- SHA-256 y no BCrypt porque el token tiene 256 bits de aleatoriedad (no hay fuerza bruta viable,
    -- un algoritmo lento no aporta nada) y porque BCrypt lleva sal aleatoria: el mismo token daría
    -- hashes distintos y no se podría BUSCAR por él.
    token_hash TEXT NOT NULL UNIQUE,

    -- VARCHAR con CHECK y no un enum de Postgres: nada en SQL lo lee, y un enum obligaría a castear
    -- en cada INSERT de JPA. Los valores en español, como el resto del esquema.
    tipo VARCHAR(20) NOT NULL,

    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_expiracion TIMESTAMPTZ NOT NULL,

    -- Un solo uso se consigue BORRANDO la fila en el canje, no marcándola: así no queda rastro de un
    -- token gastado ocupando la fila UNIQUE del usuario.
    CONSTRAINT chk_token_tipo CHECK (tipo IN ('INVITACION', 'RECUPERACION')),
    CONSTRAINT chk_expiracion_valida CHECK (fecha_expiracion > fecha_creacion)
);

-- TOKEN_CAMBIO_CORREO --
-- Cambiar el correo no puede ser un UPDATE directo: quien tuviera acceso momentáneo a una sesión
-- ajena podría apuntar la cuenta a un correo propio y adueñarse de ella desde "olvidé mi
-- contraseña", sin que el dueño real se entere de nada. El cambio se pide, se confirma desde el
-- buzón nuevo, y solo entonces se aplica.
--
-- Tabla propia y no una fila más en token_recuperacion, por dos motivos:
--   1. Hay que guardar el correo NUEVO hasta que se confirme, y aquella tabla no tiene dónde.
--   2. token_recuperacion lleva usuario_id UNIQUE, y emitir un token borra el anterior del mismo
--      usuario. Compartirla haría que pedir un cambio de correo invalidara una recuperación de
--      contraseña en curso, y al revés: son dos trámites independientes y tienen que convivir.
CREATE TABLE token_cambio_correo (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- UNIQUE por lo mismo que en token_recuperacion: pedir un cambio nuevo invalida el anterior, y
    -- así no quedan dos enlaces vivos apuntando a correos distintos.
    --
    -- ON DELETE CASCADE, y aquí sí: sp_eliminar_persona borra la cuenta y solo sabe vaciar
    -- token_recuperacion antes. Sin la cascada, esta tabla dejaría esa rutina rota en cuanto
    -- alguien con un cambio pendiente fuera eliminado.
    usuario_id INTEGER NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,

    -- El destino. No se copia a usuario.correo hasta el canje: mientras esta fila exista, la cuenta
    -- sigue funcionando con el correo de siempre.
    correo_nuevo VARCHAR(150) NOT NULL,

    -- El HASH SHA-256 del token, nunca el token, igual que en token_recuperacion: el valor original
    -- solo existe dentro del correo.
    token_hash TEXT NOT NULL UNIQUE,

    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_expiracion TIMESTAMPTZ NOT NULL,

    -- Un solo uso se consigue BORRANDO la fila al canjear, no marcándola.
    CONSTRAINT chk_cambio_correo_expiracion CHECK (fecha_expiracion > fecha_creacion)
);

-- REFRESH_TOKEN --
-- Sesión persistente: el access token (JWT) dura una hora y viaja en cada request; este dura meses y
-- solo sirve para pedir uno nuevo. Así recepción, que tiene la pantalla abierta toda la jornada, no
-- vuelve a escribir credenciales.
--
-- Por qué no basta con alargar el JWT: un token de vida larga que viaja en cada petición y que NO se
-- puede revocar es el peor caso posible si alguien lo captura. Esta tabla existe justamente para
-- poder revocar.
--
-- Tres diferencias con token_recuperacion y token_cambio_correo, y las tres son deliberadas:
CREATE TABLE refresh_token (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- 1. SIN UNIQUE, a diferencia de las otras dos tablas de token. Una persona tiene varias sesiones
    --    vivas a la vez: entra desde el celular y desde el computador de recepción, y cerrar una no
    --    puede tumbar la otra.
    usuario_id INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,

    -- Hash SHA-256, nunca el token. Igual que en el resto del sistema: el valor en claro solo existe
    -- en el dispositivo de la persona.
    token_hash TEXT NOT NULL UNIQUE,

    expira_en TIMESTAMPTZ NOT NULL,

    -- 2. Se REVOCA marcando, no borrando la fila. Las otras tablas borran porque un token gastado no
    --    dice nada; acá sí dice: si llega un refresh token que ya fue rotado, alguien está usando una
    --    copia vieja, y eso es señal de que el token se filtró. Con la fila borrada, ese intento
    --    parecería un token inventado cualquiera.
    revocado_en TIMESTAMPTZ,

    -- 3. Se guarda de dónde vino, para poder mostrar "sesiones activas" y para que revocar una sesión
    --    concreta signifique algo para quien la mira. Solo informativo: nunca se valida contra él
    --    —cambia entre versiones del navegador— y no es un factor de seguridad.
    user_agent VARCHAR(255),

    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_refresh_token_expiracion CHECK (expira_en > fecha_creacion)
);
-- Para revocar de golpe todas las sesiones de alguien: pasa al cambiar la contraseña y al desactivar
-- la cuenta. Sin índice sería un recorrido completo de la tabla cada vez.
CREATE INDEX idx_refresh_token_usuario ON refresh_token (usuario_id);

-- TARIFA --
CREATE TABLE tarifa (
    id INTEGER PRIMARY KEY DEFAULT 1,
    monto NUMERIC(10,0) NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_por INTEGER REFERENCES usuario(id),
    CONSTRAINT chk_tarifa_fila_unica CHECK (id = 1),
    CONSTRAINT chk_monto_positivo CHECK (monto > 0)
);

-- CONFIGURACION_COLA --
CREATE TABLE configuracion_cola (
    id INTEGER PRIMARY KEY DEFAULT 1,
    personas_por_turno INTEGER NOT NULL DEFAULT 1,
    minutos_margen_ingreso INTEGER NOT NULL DEFAULT 60, -- cuánto antes del inicio se puede hacer check-in
    minutos_visibilidad_pagos INTEGER NOT NULL DEFAULT 1, -- minutos que alguien aparece en la lista de "ya está pago".
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_por INTEGER REFERENCES usuario(id),
    CONSTRAINT chk_configuracion_cola_fila_unica CHECK (id = 1),
    CONSTRAINT chk_personas_por_turno_positivo CHECK (personas_por_turno >= 1),
    CONSTRAINT chk_minutos_margen_positivo CHECK (minutos_margen_ingreso >= 0),
    CONSTRAINT chk_minutos_visibilidad_positivo CHECK (minutos_visibilidad_pagos >= 0)
);

-- HISTORIAL_CAMBIO --
CREATE TABLE historial_cambio (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tabla_afectada tabla_auditable_enum NOT NULL,
    registro_id INTEGER NOT NULL,
    campo VARCHAR(50) NOT NULL,
    valor_anterior TEXT,
    valor_nuevo TEXT,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_historial_tabla_registro ON historial_cambio (tabla_afectada, registro_id);

-- Datos iniciales
INSERT INTO rol (nombre) VALUES ('ADMIN'), ('MIEMBRO'), ('PROFESOR'), ('RECEPCIONISTA'), ('PSR_TERMINAL');
INSERT INTO tarifa (id, monto) VALUES (1, 4000);
INSERT INTO configuracion_cola (id, personas_por_turno) VALUES (1, 1);
INSERT INTO metodo_pago (nombre) VALUES ('EFECTIVO'), ('TRANSFERENCIA');

-- Persona y usuario sentinel (id fijo 999999999) — objetivo: sp_eliminar_persona: conserva el hecho de registros necesarios
INSERT INTO persona (id, tipo_documento, numero_documento, nombre, apellidos, telefono, fuente_registro, activo)
    OVERRIDING SYSTEM VALUE
VALUES (999999999, 'CC', 'SENTINEL-DELETED-000', 'Usuario', 'Eliminado', '0000000000', 'ADMIN', false);

INSERT INTO usuario (id, numero_documento, correo, password_hash, persona_id, activo)
    OVERRIDING SYSTEM VALUE
VALUES (999999999, 'SENTINEL-DELETED-000', 'sentinel-deleted@psr.local', '!SENTINEL_NO_LOGIN_POSSIBLE!', 999999999, false);

-- Usuario SISTEMA (id fijo 999999998)
--
-- Objetivo: las acciones automáticas (hoy, el cierre de sesiones vencidas del scheduler) tienen que
-- quedar en historial_cambio, y esa tabla exige usuario_id NOT NULL con FK a usuario. Sin una cuenta
-- a la que atribuirlas, no habría forma de auditarlas.
--
-- No puede iniciar sesión: activo = false y el hash no corresponde a ninguna contraseña.
-- Sin persona asociada y sin roles, así que no lo alcanza trg_validar_persona_requerida.
-- Distinto del sentinel 999999999, que representa a las personas eliminadas.
INSERT INTO usuario (id, correo, password_hash, activo)
    OVERRIDING SYSTEM VALUE
VALUES (999999998, 'sistema@psr.local', '!SYSTEM_NO_LOGIN_POSSIBLE!', false);

