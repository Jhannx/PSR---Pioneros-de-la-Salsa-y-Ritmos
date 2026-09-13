-- PSR Aeróbicos — Triggers
--
-- NOTA: los triggers de auditoría necesitan saber quién está haciendo el cambio en este momento.
-- Por eso se debe ejecutar en la misma transacción, antes de cualquier UPDATE sobre asistencia, movimiento_cuenta, tarifa o configuracion_cola:
-- Ejecutar: SELECT set_config('app.usuario_id', '<id_del_usuario_logueado>', true);
-- Si no se configuró, la operación falla. Es intencional para mantener la auditoría.

CREATE OR REPLACE FUNCTION fn_usuario_actual()
    RETURNS INTEGER AS $$
DECLARE
    v_valor TEXT;
BEGIN
    v_valor := current_setting('app.usuario_id', true);

    IF v_valor IS NULL OR v_valor = '' THEN
        RAISE EXCEPTION 'app.usuario_id no está configurado en esta transacción. '
            'Ejecuta SELECT set_config(''app.usuario_id'', ''<id>'', true) antes de modificar datos auditables.';
    END IF;

    RETURN v_valor::INTEGER;
END;
$$ LANGUAGE plpgsql STABLE;

-- 1) trg_auditar_movimiento_cuenta — se dispara solo si alguien corrige un pago YA registrado
CREATE OR REPLACE FUNCTION fn_trg_auditar_movimiento_cuenta()
    RETURNS TRIGGER AS $$
DECLARE
    v_usuario_id INTEGER;
BEGIN
    v_usuario_id := fn_usuario_actual();

    IF NEW.monto_recibido IS DISTINCT FROM OLD.monto_recibido THEN
        INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
        VALUES ('MOVIMIENTO_CUENTA', OLD.id, 'monto_recibido',
                OLD.monto_recibido::TEXT, NEW.monto_recibido::TEXT, v_usuario_id);
    END IF;

    IF NEW.monto_abonado IS DISTINCT FROM OLD.monto_abonado THEN
        INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
        VALUES ('MOVIMIENTO_CUENTA', OLD.id, 'monto_abonado',
                OLD.monto_abonado::TEXT, NEW.monto_abonado::TEXT, v_usuario_id);
    END IF;

    IF NEW.metodo_pago_id IS DISTINCT FROM OLD.metodo_pago_id THEN
        INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
        VALUES ('MOVIMIENTO_CUENTA', OLD.id, 'metodo_pago_id',
                OLD.metodo_pago_id::TEXT, NEW.metodo_pago_id::TEXT, v_usuario_id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_auditar_movimiento_cuenta
    AFTER UPDATE ON movimiento_cuenta
    FOR EACH ROW
EXECUTE FUNCTION fn_trg_auditar_movimiento_cuenta();

-- 2) trg_auditar_asistencia — se dispara solo si alguien correge el monto ya cobrado de una asistencia
CREATE OR REPLACE FUNCTION fn_trg_auditar_asistencia()
    RETURNS TRIGGER AS $$
DECLARE
    v_usuario_id INTEGER;
BEGIN
    IF NEW.monto_cobrado IS DISTINCT FROM OLD.monto_cobrado THEN
        v_usuario_id := fn_usuario_actual();
        INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
        VALUES ('ASISTENCIA', OLD.id, 'monto_cobrado',
                OLD.monto_cobrado::TEXT, NEW.monto_cobrado::TEXT, v_usuario_id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_auditar_asistencia
    AFTER UPDATE ON asistencia
    FOR EACH ROW
EXECUTE FUNCTION fn_trg_auditar_asistencia();

-- 3) trg_tarifa_actualizar_y_auditar — el timestamp se pone SIEMPRE (mantener registro de edición); el registro de auditoría solo si el precio realmente cambió
CREATE OR REPLACE FUNCTION fn_trg_tarifa_actualizar_y_auditar()
    RETURNS TRIGGER AS $$
DECLARE
    v_usuario_id INTEGER;
BEGIN
    NEW.actualizado_en := now();

    IF NEW.monto IS DISTINCT FROM OLD.monto THEN
        v_usuario_id := fn_usuario_actual();
        INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
        VALUES ('TARIFA', OLD.id, 'monto', OLD.monto::TEXT, NEW.monto::TEXT, v_usuario_id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_tarifa_actualizar_y_auditar
    BEFORE UPDATE ON tarifa
    FOR EACH ROW
EXECUTE FUNCTION fn_trg_tarifa_actualizar_y_auditar();

-- 4) trg_validar_persona_requerida — MIEMBRO, PROFESOR y RECEPCIONISTA representan a alguien real en
-- el sistema (asiste, dicta, atiende); solo ADMIN puede ser una cuenta técnica sin persona detrás.
--
-- Cuidado al tocar esta lista: Postgres castea los literales del IN al tipo de la variable, así que un
-- valor que no esté en rol_nombre_enum revienta con "invalid input value for enum". Y como el trigger
-- es BEFORE INSERT ON usuario_rol, eso tumba TODA alta de usuario — incluida la que hace
-- sp_registrar_usuario_para_persona al asignar el rol MIEMBRO. Agregar primero al enum, luego aquí.
CREATE OR REPLACE FUNCTION fn_trg_validar_persona_requerida()
    RETURNS TRIGGER AS $$
DECLARE
    v_rol_nombre rol_nombre_enum;
    v_persona_id INTEGER;
BEGIN
    SELECT nombre INTO v_rol_nombre FROM rol WHERE id = NEW.rol_id;

    IF v_rol_nombre IN ('MIEMBRO', 'PROFESOR', 'RECEPCIONISTA') THEN
        SELECT persona_id INTO v_persona_id FROM usuario WHERE id = NEW.usuario_id;

        IF v_persona_id IS NULL THEN
            RAISE EXCEPTION 'Un usuario con rol % debe estar asociado a una persona', v_rol_nombre;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_validar_persona_requerida
    BEFORE INSERT OR UPDATE ON usuario_rol
    FOR EACH ROW
EXECUTE FUNCTION fn_trg_validar_persona_requerida();

-- 5) trg_sincronizar_usuario_con_persona — evita que alguien quede reactivado en persona pero con su cuenta todavía bloqueada
CREATE OR REPLACE FUNCTION fn_trg_sincronizar_usuario_con_persona()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.activo IS DISTINCT FROM OLD.activo
        OR NEW.numero_documento IS DISTINCT FROM OLD.numero_documento THEN
        UPDATE usuario
        SET activo = NEW.activo,
            numero_documento = NEW.numero_documento
        WHERE persona_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_sincronizar_usuario_con_persona
    AFTER UPDATE ON persona
    FOR EACH ROW
EXECUTE FUNCTION fn_trg_sincronizar_usuario_con_persona();

-- 6) trg_configuracion_cola_actualizar_y_auditar — audita personas_por_turno, minutos_margen_ingreso y minutos_visibilidad_pagos por separado; el timestamp se pone siempre
CREATE OR REPLACE FUNCTION fn_trg_configuracion_cola_actualizar_y_auditar()
    RETURNS TRIGGER AS $$
DECLARE
    v_usuario_id INTEGER;
BEGIN
    NEW.actualizado_en := now();

    IF NEW.personas_por_turno IS DISTINCT FROM OLD.personas_por_turno THEN
        v_usuario_id := fn_usuario_actual();
        INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
        VALUES ('CONFIGURACION_COLA', OLD.id, 'personas_por_turno',
                OLD.personas_por_turno::TEXT, NEW.personas_por_turno::TEXT, v_usuario_id);
    END IF;

    IF NEW.minutos_margen_ingreso IS DISTINCT FROM OLD.minutos_margen_ingreso THEN
        v_usuario_id := fn_usuario_actual();
        INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
        VALUES ('CONFIGURACION_COLA', OLD.id, 'minutos_margen_ingreso',
                OLD.minutos_margen_ingreso::TEXT, NEW.minutos_margen_ingreso::TEXT, v_usuario_id);
    END IF;

    IF NEW.minutos_visibilidad_pagos IS DISTINCT FROM OLD.minutos_visibilidad_pagos THEN
        v_usuario_id := fn_usuario_actual();
        INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
        VALUES ('CONFIGURACION_COLA', OLD.id, 'minutos_visibilidad_pagos',
                OLD.minutos_visibilidad_pagos::TEXT, NEW.minutos_visibilidad_pagos::TEXT, v_usuario_id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_configuracion_cola_actualizar_y_auditar
    BEFORE UPDATE ON configuracion_cola
    FOR EACH ROW
EXECUTE FUNCTION fn_trg_configuracion_cola_actualizar_y_auditar();

-- 7) trg_validar_dia_excepcion — la fecha de una excepción tiene que caer en el día de la semana que
-- declara su horario. Sin esto se podía guardar una excepción para el horario de los LUNES con una
-- fecha que cae jueves: la excepción no cancelaba nada, porque ese jueves no había sesión de ese
-- horario, y el lunes real seguía generándose. Un error silencioso — nadie se entera hasta que la
-- clase que se quiso cancelar se da igual.
--
-- ISODOW y no DOW: ISODOW numera 1=lunes … 7=domingo, que es exactamente la convención de
-- horario.dia_semana (documentada en V1). DOW numera 0=domingo … 6=sábado y estaría desplazado uno.
--
-- El frontend además restringe el date-picker para que no se llegue aquí en uso normal, y
-- ScheduleExceptionService valida lo mismo para responder un 400 legible. Este trigger es la red de
-- seguridad: garantiza el dato aunque alguien escriba por SQL, saltándose las dos capas de arriba.
CREATE OR REPLACE FUNCTION fn_trg_validar_dia_excepcion()
    RETURNS TRIGGER AS $$
DECLARE
    v_dia_horario INTEGER;
    v_dia_fecha   INTEGER;
BEGIN
    SELECT dia_semana INTO v_dia_horario FROM horario WHERE id = NEW.horario_id;

    -- La FK ya lo garantiza; esto es por si el trigger sobrevive a un cambio de esquema futuro.
    IF v_dia_horario IS NULL THEN
        RAISE EXCEPTION 'El horario % no existe', NEW.horario_id;
    END IF;

    v_dia_fecha := EXTRACT(ISODOW FROM NEW.fecha);

    IF v_dia_fecha <> v_dia_horario THEN
        RAISE EXCEPTION
            'La fecha % cae en el día % y el horario % es del día %',
            NEW.fecha, v_dia_fecha, NEW.horario_id, v_dia_horario;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_validar_dia_excepcion
    BEFORE INSERT OR UPDATE ON horario_excepcion
    FOR EACH ROW
EXECUTE FUNCTION fn_trg_validar_dia_excepcion();

-- 8) trg_limitar_contactos_emergencia — tope de cinco contactos por persona.
--
-- Trigger y no CHECK, y no es una preferencia: un CHECK solo puede mirar la fila que se está
-- insertando, no contar las que ya hay en la tabla. Contar filas exige una consulta, y eso solo cabe
-- en un trigger.
--
-- Solo en INSERT: un UPDATE no puede aumentar el total de una persona salvo que mueva el contacto de
-- una persona a otra, cosa que ningún endpoint hace (PUT /api/contactos-emergencia/{id} edita nombre
-- y teléfono, nunca persona_id).
--
-- EmergencyContactService cuenta también antes de insertar, para devolver un 409 con un mensaje que
-- se pueda leer. Este trigger es lo que garantiza el tope pase lo que pase.
CREATE OR REPLACE FUNCTION fn_trg_limitar_contactos_emergencia()
    RETURNS TRIGGER AS $$
DECLARE
    v_total INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
    FROM contacto_emergencia
    WHERE persona_id = NEW.persona_id;

    IF v_total >= 5 THEN
        RAISE EXCEPTION 'La persona % ya tiene 5 contactos de emergencia', NEW.persona_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trg_limitar_contactos_emergencia
    BEFORE INSERT ON contacto_emergencia
    FOR EACH ROW
EXECUTE FUNCTION fn_trg_limitar_contactos_emergencia();
