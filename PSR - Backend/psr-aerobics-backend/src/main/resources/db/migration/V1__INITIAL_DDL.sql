-- Tipos de datos (ENUMS) y tablas

-- Tipos de datos y extensiones
-- PSR Aeróbicos — Extensiones y tipos (enums)

CREATE EXTENSION IF NOT EXISTS pgcrypto; -- necesaria para gen_random_bytes() en sp_solicitar_recuperacion

CREATE TYPE fuente_registro_enum AS ENUM (
    'ADMIN', 'AUTOREGISTRO'
    );

CREATE TYPE tipo_documento_enum AS ENUM (
    'CC', 'TI', 'CE', 'PA'
    );

CREATE TYPE metodo_checkin_enum AS ENUM (
    'QR', 'DOCUMENTO', 'MANUAL'
    );

CREATE TYPE tipo_movimiento_enum AS ENUM (
    'ABONO', 'REEMBOLSO'
    );

CREATE TYPE tabla_auditable_enum AS ENUM (
    'ASISTENCIA', 'MOVIMIENTO_CUENTA', 'TARIFA', 'CONFIGURACION_COLA', 'PERSONA', 'SESION'
    );

CREATE TYPE rol_nombre_enum AS ENUM (
    'ADMIN', 'MIEMBRO', 'PROFESOR'
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
    fuente_registro fuente_registro_enum NOT NULL,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT now(),
    activo BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_persona_documento UNIQUE (numero_documento)
);

-- CONTACTO_EMERGENCIA --
CREATE TABLE contacto_emergencia (
    id  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    persona_id INTEGER NOT NULL UNIQUE REFERENCES persona(id),
    nombre VARCHAR(150),
    telefono VARCHAR(20),
    observaciones TEXT,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_contacto_emergencia_no_vacio
    CHECK (nombre IS NOT NULL OR telefono IS NOT NULL OR observaciones IS NOT NULL)
);

-- USUARIO --
CREATE TABLE usuario (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_documento VARCHAR(20) UNIQUE,
    correo VARCHAR(150) UNIQUE,
    password_hash TEXT NOT NULL,
    persona_id INTEGER REFERENCES persona(id),
    activo BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_usuario_identificador CHECK (numero_documento IS NOT NULL OR correo IS NOT NULL)
);
CREATE INDEX idx_usuario_persona ON usuario (persona_id);

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
    monto_cobrado NUMERIC(10,2) NOT NULL,
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
    monto_recibido NUMERIC(10,2) NOT NULL,
    monto_abonado NUMERIC(10,2) NOT NULL,
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
CREATE TABLE token_recuperacion (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    token TEXT NOT NULL UNIQUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT chk_expiracion_valida CHECK (fecha_expiracion > fecha_creacion)
);
CREATE INDEX idx_token_usuario ON token_recuperacion (usuario_id);

-- TARIFA --
CREATE TABLE tarifa (
    id INTEGER PRIMARY KEY DEFAULT 1,
    monto NUMERIC(10,2) NOT NULL,
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
    minutos_visibilidad_pagos INTEGER NOT NULL DEFAULT 2, -- minutos que alguien aparece en la lista de "ya está pago".
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
INSERT INTO rol (nombre) VALUES ('ADMIN'), ('MIEMBRO'), ('PROFESOR');
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

