-- PSR Aeróbicos — Procedimientos

-- sp_registrar_persona --
-- DROP explícito antes de crear, por la misma razón que en sp_registrar_usuario_para_persona: en
-- Postgres los parámetros OUT forman parte de la identidad de un procedimiento, así que agregar
-- p_fecha_nacimiento NO reemplaza al anterior — crea una SOBRECARGA, y el CALL de seis argumentos
-- queda ambiguo. El síntoma no apunta al procedimiento: lo que se ve es que toda alta de persona
-- deja de funcionar. Sin lista de tipos porque solo hay uno con este nombre.
DROP PROCEDURE IF EXISTS sp_registrar_persona;

-- Sin DEFAULT en p_fecha_nacimiento, a propósito: un parámetro con valor por defecto delante de un
-- OUT es terreno resbaladizo en Postgres, y no hace falta. El único que llama a este procedimiento
-- es PersonProcedureRepository, que siempre pasa el argumento —null cuando no hay fecha—, así que
-- la columna sigue admitiendo NULL sin necesidad de que el parámetro sea opcional.
CREATE OR REPLACE PROCEDURE sp_registrar_persona(
    IN  p_tipo_documento tipo_documento_enum, IN p_numero_documento VARCHAR(20),
    IN  p_nombre VARCHAR(100), IN p_apellidos VARCHAR(100), IN p_telefono VARCHAR(20),
    IN  p_fecha_nacimiento DATE,
    IN  p_fuente_registro fuente_registro_enum,
    OUT p_persona_id INTEGER
)
    LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO persona (tipo_documento, numero_documento, nombre, apellidos, telefono,
                         fuente_registro, fecha_nacimiento)
    VALUES (p_tipo_documento, p_numero_documento, p_nombre, p_apellidos, p_telefono,
            p_fuente_registro, p_fecha_nacimiento)
    RETURNING id INTO p_persona_id;
END;
$$;

-- sp_registrar_usuario_para_persona --
-- Crea la cuenta de acceso (rol MIEMBRO) para una persona que ya existe.
-- DROP explícito antes de crear: en Postgres los OUT forman parte de la identidad de un
-- procedimiento, así que agregar p_correo NO reemplaza al anterior, crea una sobrecarga y el CALL
-- de tres argumentos queda ambiguo. Sin lista de tipos porque solo hay uno con este nombre.
DROP PROCEDURE IF EXISTS sp_registrar_usuario_para_persona;

CREATE OR REPLACE PROCEDURE sp_registrar_usuario_para_persona(
    IN  p_persona_id INTEGER, IN p_password_hash TEXT, IN p_correo VARCHAR(150),
    OUT p_usuario_id INTEGER
)
    LANGUAGE plpgsql AS $$
BEGIN
    -- numero_documento se deriva de persona, no se recibe como parámetro — evita que quede
    -- desincronizado del documento real de la persona a la que pertenece la cuenta.
    IF NOT EXISTS (SELECT 1 FROM persona WHERE id = p_persona_id) THEN
        RAISE EXCEPTION 'La persona % no existe', p_persona_id;
    END IF;

    -- El correo sí llega como parámetro: es un dato de la CUENTA, no de la persona, y es opcional.
    -- Sin él la cuenta se identifica solo por documento, que es lo que permite
    -- chk_usuario_identificador.
    INSERT INTO usuario (numero_documento, correo, password_hash, persona_id)
    SELECT numero_documento, p_correo, p_password_hash, id FROM persona WHERE id = p_persona_id
    RETURNING id INTO p_usuario_id;

    -- Si se necesitan más roles se agrega desde sp_asignar_rol, inicialmente todo usuario es un miembro
    INSERT INTO usuario_rol (usuario_id, rol_id)
    VALUES (p_usuario_id, (SELECT id FROM rol WHERE nombre = 'MIEMBRO'));
END;
$$;

-- sp_asignar_rol --
CREATE OR REPLACE PROCEDURE sp_asignar_rol(
    IN p_usuario_id INTEGER, IN p_rol_nombre rol_nombre_enum
)
    LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO usuario_rol (usuario_id, rol_id)
    VALUES (p_usuario_id, (SELECT id FROM rol WHERE nombre = p_rol_nombre))
    ON CONFLICT (usuario_id, rol_id) DO NOTHING;
END;
$$;

-- sp_quitar_rol Quita un rol. No permite dejar a un usuario sin ningún rol
CREATE OR REPLACE PROCEDURE sp_quitar_rol(
    IN p_usuario_id INTEGER, IN p_rol_nombre rol_nombre_enum
)
    LANGUAGE plpgsql AS $$
DECLARE
    v_roles_restantes INTEGER;
BEGIN
    SELECT count(*) INTO v_roles_restantes
    FROM usuario_rol ur
             JOIN rol r ON r.id = ur.rol_id
    WHERE ur.usuario_id = p_usuario_id AND r.nombre <> p_rol_nombre;

    IF v_roles_restantes = 0 THEN
        RAISE EXCEPTION 'No se puede quitar el último rol del usuario %', p_usuario_id;
    END IF;

    DELETE FROM usuario_rol
    WHERE usuario_id = p_usuario_id
      AND rol_id = (SELECT id FROM rol WHERE nombre = p_rol_nombre);
END;
$$;

-- sp_reactivar_persona --
CREATE OR REPLACE PROCEDURE sp_reactivar_persona(
    IN p_persona_id INTEGER, IN p_ejecutado_por_usuario_id INTEGER
)
    LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM persona WHERE id = p_persona_id) THEN
        RAISE EXCEPTION 'La persona % no existe', p_persona_id;
    END IF;

    UPDATE persona SET activo = true WHERE id = p_persona_id;

    INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
    VALUES ('PERSONA', p_persona_id, 'activo', 'false', 'true', p_ejecutado_por_usuario_id);
END;
$$;

-- sp_registrar_llegada -- Entra a la fila de la sesión vigente en este momento. Usa la sesión con fn_obtener_o_crear_sesion_del_momento()
CREATE OR REPLACE PROCEDURE sp_registrar_llegada(
    IN  p_persona_id INTEGER, IN p_metodo_checkin metodo_checkin_enum,
    OUT p_llegada_id INTEGER
)
    LANGUAGE plpgsql AS $$
DECLARE
    v_sesion_id INTEGER;
BEGIN
    PERFORM fn_validar_persona_activa(p_persona_id);

    -- Solo el check-in MANUAL puede entrar fuera de horario, y el permiso sale del metodo y no de un
    -- parametro nuevo a proposito: asi no hay forma de pedir la excepcion desde un canal que no
    -- deberia tenerla. MANUAL lo registra una persona del mostrador que tiene delante a quien llega;
    -- el propio del miembro, el QR y el de documento de la terminal son automaticos y se rigen por la
    -- regla sin excepciones.
    v_sesion_id := fn_obtener_o_crear_sesion_del_momento(p_metodo_checkin = 'MANUAL');

    SELECT id INTO p_llegada_id
    FROM llegada
    WHERE persona_id = p_persona_id AND sesion_id = v_sesion_id;

    IF p_llegada_id IS NULL THEN
        INSERT INTO llegada (persona_id, sesion_id, hora_llegada, metodo_checkin)
        VALUES (p_persona_id, v_sesion_id, now(), p_metodo_checkin)
        RETURNING id INTO p_llegada_id;
    END IF;
END;
$$;

-- sp_crear_sesion_manual -- El admin crea una sesión directamente, en cualquier momento
CREATE OR REPLACE PROCEDURE sp_crear_sesion_manual(
    IN  p_fecha DATE, IN p_hora_inicio TIMESTAMPTZ, IN p_hora_fin TIMESTAMPTZ,
    IN  p_profesor_usuario_id INTEGER, IN p_horario_id INTEGER,
    OUT p_sesion_id INTEGER
)
    LANGUAGE plpgsql AS $$
BEGIN
    IF p_horario_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM sesion WHERE horario_id = p_horario_id AND fecha = p_fecha
    ) THEN
        RAISE EXCEPTION 'Ya existe una sesión para el horario % en la fecha %', p_horario_id, p_fecha;
    END IF;

    -- Quien dicta la clase debe ser PROFESOR o ADMIN.
    --
    -- ADMIN entra aquí a partir del 13-sep: en una academia pequeña quien administra también da
    -- clase, y obligar a darse a sí mismo el rol PROFESOR para poder aparecer en el selector era
    -- papeleo que además ensucia los permisos. El nombre del parámetro se queda como
    -- p_profesor_usuario_id para no romper a quien ya lo llama.
    IF p_profesor_usuario_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM usuario_rol ur
                          JOIN rol r ON r.id = ur.rol_id
        WHERE ur.usuario_id = p_profesor_usuario_id
          AND r.nombre IN ('PROFESOR', 'ADMIN')
    ) THEN
        RAISE EXCEPTION 'El usuario % no puede dictar clase: necesita rol PROFESOR o ADMIN', p_profesor_usuario_id;
    END IF;

    INSERT INTO sesion (horario_id, fecha, hora_inicio, hora_fin, profesor_usuario_id)
    VALUES (p_horario_id, p_fecha, p_hora_inicio, p_hora_fin, p_profesor_usuario_id)
    RETURNING id INTO p_sesion_id;
END;
$$;

-- sp_cancelar_sesion --
CREATE OR REPLACE PROCEDURE sp_cancelar_sesion(
    IN p_sesion_id INTEGER, IN p_ejecutado_por_usuario_id INTEGER
)
    LANGUAGE plpgsql AS $$
BEGIN
    UPDATE sesion SET cancelada = true WHERE id = p_sesion_id AND cancelada = false;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La sesión % no existe o ya estaba cancelada', p_sesion_id;
    END IF;

    INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
    VALUES ('SESION', p_sesion_id, 'cancelada', 'false', 'true', p_ejecutado_por_usuario_id);
END;
$$;

-- sp_registrar_asistencia --
CREATE OR REPLACE PROCEDURE sp_registrar_asistencia(
    IN p_llegada_id INTEGER, IN p_usuario_id INTEGER
)
    LANGUAGE plpgsql AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    SELECT EXISTS(SELECT 1 FROM llegada WHERE id = p_llegada_id) INTO v_existe;

    IF NOT v_existe THEN
        RAISE EXCEPTION 'La llegada % no existe', p_llegada_id;
    END IF;

    INSERT INTO asistencia (llegada_id, monto_cobrado, marcado_por)
    VALUES (p_llegada_id, fn_tarifa_vigente(), p_usuario_id);
EXCEPTION
    WHEN unique_violation THEN
        RAISE EXCEPTION 'La llegada % ya fue atendida', p_llegada_id;
END;
$$;

-- sp_anular_asistencia --
CREATE OR REPLACE PROCEDURE sp_anular_asistencia(
    IN p_llegada_id INTEGER
)
    LANGUAGE plpgsql AS $$
BEGIN
    DELETE FROM asistencia WHERE llegada_id = p_llegada_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La llegada % no tiene una asistencia registrada', p_llegada_id;
    END IF;
END;
$$;

-- sp_registrar_movimiento --
-- Registrar un pago (abono) completamente independiente de si la persona ya fue atendida o ni siquiera ha llegado (pago por adelantado donde p_llegada_id puede ser NULL).
CREATE OR REPLACE PROCEDURE sp_registrar_movimiento(
    IN p_persona_id INTEGER, IN p_llegada_id INTEGER, IN p_monto_recibido NUMERIC,
    IN p_monto_abonado NUMERIC, IN p_usuario_id INTEGER, IN p_metodo_pago_id INTEGER,
    IN p_nota TEXT DEFAULT NULL
)
    LANGUAGE plpgsql AS $$
BEGIN
    PERFORM fn_validar_persona_activa(p_persona_id);

    INSERT INTO movimiento_cuenta
    (persona_id, llegada_id, monto_recibido, monto_abonado, metodo_pago_id, tipo_movimiento, usuario_id, nota)
    VALUES
        (p_persona_id, p_llegada_id, p_monto_recibido, p_monto_abonado, p_metodo_pago_id, 'ABONO', p_usuario_id, p_nota);
END;
$$;

-- sp_registrar_reembolso --
-- Dinero que entró y después salió
CREATE OR REPLACE PROCEDURE sp_registrar_reembolso(
    IN p_movimiento_original_id INTEGER, IN p_monto NUMERIC, IN p_usuario_id INTEGER,
    IN p_metodo_pago_id INTEGER, IN p_nota TEXT DEFAULT NULL
)
    LANGUAGE plpgsql AS $$
DECLARE
    v_persona_id        INTEGER;
    v_llegada_id        INTEGER;
    v_monto_original    NUMERIC;
    v_ya_reembolsado    NUMERIC;
    v_disponible        NUMERIC;
BEGIN
    SELECT persona_id, llegada_id, monto_abonado
      INTO v_persona_id, v_llegada_id, v_monto_original
    FROM movimiento_cuenta
    WHERE id = p_movimiento_original_id AND tipo_movimiento = 'ABONO';

    IF v_persona_id IS NULL THEN
        RAISE EXCEPTION 'El movimiento % no existe o no es un abono reembolsable', p_movimiento_original_id;
    END IF;

    IF p_monto <= 0 THEN
        RAISE EXCEPTION 'El monto del reembolso debe ser positivo';
    END IF;

    -- Lo ya devuelto contra ESTE abono. Los reembolsos guardan monto_abonado en negativo, de ahí el
    -- signo cambiado al sumarlos.
    --
    -- Sin esta resta el tope se comparaba siempre contra el abono entero, así que un mismo abono de
    -- 2500 se podía reembolsar cuantas veces se quisiera, 2500 cada vez: la validación parecía
    -- proteger y no protegía nada. Es un agujero de dinero, no una imprecisión.
    SELECT COALESCE(SUM(-monto_abonado), 0) INTO v_ya_reembolsado
    FROM movimiento_cuenta
    WHERE movimiento_original_id = p_movimiento_original_id
      AND tipo_movimiento = 'REEMBOLSO';

    v_disponible := v_monto_original - v_ya_reembolsado;

    IF p_monto > v_disponible THEN
        RAISE EXCEPTION 'El reembolso (%) supera lo que queda por devolver de ese abono (%). Abonado: %, ya devuelto: %',
            p_monto, v_disponible, v_monto_original, v_ya_reembolsado;
    END IF;

    -- El reembolso hereda la llegada del abono que devuelve, y no es cosmético: vista_fila_hoy saca
    -- la nota de la fila del último movimiento DE ESA LLEGADA. Con llegada_id = NULL el reembolso
    -- nunca entraba en ese LATERAL, así que la fila seguía anunciando "cobro de entrada + 10 días por
    -- adelantado" después de haber devuelto el dinero — contaba algo que ya no era cierto.
    --
    -- NULL cuando el abono tampoco colgaba de ninguna llegada (un pago por adelantado, por ejemplo):
    -- se hereda lo que haya, no se inventa.
    INSERT INTO movimiento_cuenta
    (persona_id, llegada_id, monto_recibido, monto_abonado, metodo_pago_id, tipo_movimiento, movimiento_original_id, usuario_id, nota)
    VALUES
        (v_persona_id, v_llegada_id, 0, -p_monto, p_metodo_pago_id, 'REEMBOLSO', p_movimiento_original_id, p_usuario_id, p_nota);
END;
$$;

-- sp_entregar_vuelto --
CREATE OR REPLACE PROCEDURE sp_entregar_vuelto(
    IN p_movimiento_id INTEGER
)
    LANGUAGE plpgsql AS $$
BEGIN
    UPDATE movimiento_cuenta
    SET vuelto_entregado = true
    WHERE id = p_movimiento_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El movimiento % no existe', p_movimiento_id;
    END IF;
END;
$$;

-- sp_eliminar_persona --
-- Eliminación real de persona y usuario. Antes de borrar, reasigna su huella (llegada,
-- movimiento_cuenta, asistencia, historial_cambio, sesion, tarifa, configuracion_cola) a la
-- persona/usuario sentinel (id 999999999): el hecho permanece, la identidad no. Exige force=true.
--
-- contacto_emergencia y consentimiento no se reasignan: se borran. Ver el comentario de cada DELETE.
CREATE OR REPLACE PROCEDURE sp_eliminar_persona(
    IN p_persona_id INTEGER, IN p_ejecutado_por_usuario_id INTEGER, IN p_force BOOLEAN
)
    LANGUAGE plpgsql AS $$
DECLARE
    v_usuario_id INTEGER;
BEGIN
    IF NOT p_force THEN
        -- 75000: habla del procedimiento y de su parámetro, no de nada que el usuario pueda corregir.
        RAISE EXCEPTION 'sp_eliminar_persona exige force = true — esta operación es irreversible'
            USING ERRCODE = '75000';
    END IF;

    IF p_persona_id = 999999999 THEN
        RAISE EXCEPTION 'La persona % es el registro sentinel y no puede eliminarse', p_persona_id;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM persona WHERE id = p_persona_id) THEN
        RAISE EXCEPTION 'La persona % no existe', p_persona_id;
    END IF;

    SELECT id INTO v_usuario_id FROM usuario WHERE persona_id = p_persona_id;

    PERFORM set_config('app.usuario_id', p_ejecutado_por_usuario_id::TEXT, true);

    UPDATE movimiento_cuenta SET persona_id = 999999999 WHERE persona_id = p_persona_id;
    UPDATE llegada           SET persona_id = 999999999 WHERE persona_id = p_persona_id;

    IF v_usuario_id IS NOT NULL THEN
        UPDATE historial_cambio   SET usuario_id          = 999999999 WHERE usuario_id = v_usuario_id;
        UPDATE asistencia         SET marcado_por         = 999999999 WHERE marcado_por = v_usuario_id;
        UPDATE movimiento_cuenta  SET usuario_id          = 999999999 WHERE usuario_id = v_usuario_id;
        UPDATE sesion             SET profesor_usuario_id = 999999999 WHERE profesor_usuario_id = v_usuario_id;
        UPDATE tarifa             SET actualizado_por     = 999999999 WHERE actualizado_por = v_usuario_id;
        UPDATE configuracion_cola SET actualizado_por     = 999999999 WHERE actualizado_por = v_usuario_id;
    END IF;

    DELETE FROM contacto_emergencia WHERE persona_id = p_persona_id;

    -- Los consentimientos se borran con la persona, no se reasignan al sentinel como el dinero.
    --
    -- No es simetría rota: un movimiento reasignado sigue diciendo algo cierto ("entró este dinero"),
    -- pero una autorización de la que ya no se sabe quién la dio no prueba absolutamente nada — y una
    -- fila de consentimiento que no identifica al titular es, además, justo lo contrario de lo que la
    -- tabla existe para hacer. El numeral 8.2 de la política ya promete que en la eliminación
    -- definitiva "los datos de identificación se eliminan de forma permanente"; esto lo cumple.
    --
    -- Sin este DELETE el procedimiento entero falla: consentimiento.persona_id es FK contra persona.
    DELETE FROM consentimiento WHERE persona_id = p_persona_id;

    IF v_usuario_id IS NOT NULL THEN
        DELETE FROM token_recuperacion WHERE usuario_id = v_usuario_id;
        DELETE FROM usuario WHERE id = v_usuario_id;
    END IF;

    DELETE FROM persona WHERE id = p_persona_id;

    -- Si quien ejecuta el borrado es la propia cuenta que se acaba de eliminar (autoeliminación),
    -- p_ejecutado_por_usuario_id ya no existe en `usuario` — se deja constancia con el sentinel.
    INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
    VALUES ('PERSONA', p_persona_id, 'persona', 'activa', 'eliminada_definitivamente',
            CASE WHEN v_usuario_id = p_ejecutado_por_usuario_id THEN 999999999
                 ELSE p_ejecutado_por_usuario_id END);
END;
$$;

-- sp_cerrar_sesiones_vencidas --
-- Cierra las sesiones que quedaron abiertas (hora_fin NULL) pasadas p_horas_gracia desde su fin
-- previsto. Solo alcanza a las sesiones creadas sin hora de fin: las que ya la tienen dejan de estar
-- vigentes solas, porque fn_sesion_abierta_ahora() las descarta al pasar esa hora.
--
-- El fin previsto sale del horario asociado, si lo hay. Para una sesión suelta no hay referencia, y
-- se asume la duración típica de una clase (1 hora) desde el inicio.
--
-- Cada cierre queda en historial_cambio a nombre del usuario SISTEMA, para distinguirlo de un
-- cierre hecho por un admin.
CREATE OR REPLACE PROCEDURE sp_cerrar_sesiones_vencidas(
    IN  p_horas_gracia INTEGER,
    OUT p_cerradas     INTEGER
)
    LANGUAGE plpgsql AS $$
DECLARE
    v_sesion RECORD;
    v_ahora  TIMESTAMPTZ := now();
BEGIN
    IF p_horas_gracia IS NULL OR p_horas_gracia < 0 THEN
        RAISE EXCEPTION 'Las horas de gracia deben ser un entero no negativo';
    END IF;

    p_cerradas := 0;

    FOR v_sesion IN
        SELECT s.id
        FROM sesion s
                 LEFT JOIN horario h ON h.id = s.horario_id
        WHERE s.hora_fin IS NULL
          AND NOT s.cancelada
          AND v_ahora > COALESCE((s.fecha + h.hora_fin)::timestamptz,
                                 s.hora_inicio + interval '1 hour')
                        + make_interval(hours => p_horas_gracia)
        ORDER BY s.id
        LOOP
            UPDATE sesion SET hora_fin = v_ahora WHERE id = v_sesion.id;

            INSERT INTO historial_cambio (tabla_afectada, registro_id, campo, valor_anterior, valor_nuevo, usuario_id)
            VALUES ('SESION', v_sesion.id, 'hora_fin', NULL, v_ahora::TEXT, 999999998);

            p_cerradas := p_cerradas + 1;
        END LOOP;
END;
$$;

-- Los tokens de contraseña ya no se manejan aquí.
--
-- sp_solicitar_recuperacion y sp_restablecer_password ya no existen. Los
-- reemplaza PasswordTokenService en Java, que guarda el SHA-256 del token en vez del token en claro y
-- aplica ventanas distintas según el tipo (invitación larga, recuperación corta). Esa política
-- necesita conocer la contraseña en claro para hashearla con BCrypt, cosa que no puede hacerse desde
-- plpgsql.
