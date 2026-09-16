-- PSR Aeróbicos — Funciones

-- fn_tarifa_vigente: precio actual (fila única, id=1). La usa sp_registrar_asistencia para cobrar al confirmar, y fn_dias_credito_disponible.
CREATE OR REPLACE FUNCTION fn_tarifa_vigente()
    RETURNS NUMERIC AS $$
DECLARE
    v_monto NUMERIC;
BEGIN
    SELECT monto INTO v_monto FROM tarifa WHERE id = 1;

    IF v_monto IS NULL THEN
        -- 75000: es un fallo de instalación —falta una fila que el esquema da por hecha—, y el
        -- mensaje habla de tablas e ids. Sale como 500 y queda en el log.
        RAISE EXCEPTION 'No hay tarifa configurada (falta la fila id=1 en tarifa)'
            USING ERRCODE = '75000';
    END IF;

    RETURN v_monto;
END;
$$ LANGUAGE plpgsql STABLE;

-- fn_dias_credito_disponible: cuántos días completos cubre el saldo a favor al precio de hoy.
CREATE OR REPLACE FUNCTION fn_dias_credito_disponible(p_persona_id INTEGER)
    RETURNS INTEGER AS $$
DECLARE
    v_saldo NUMERIC;
BEGIN
    SELECT saldo INTO v_saldo FROM vista_saldo_persona WHERE persona_id = p_persona_id;

    IF v_saldo IS NULL OR v_saldo <= 0 THEN
        RETURN 0;
    END IF;

    RETURN FLOOR(v_saldo / fn_tarifa_vigente())::INTEGER;
END;
$$ LANGUAGE plpgsql STABLE;

-- fn_validar_persona_activa: revienta si no existe, o con SQLSTATE 75001 si está desactivada — esto para diferenciar en si existe la persona en la bd (para reactivar) o no.
CREATE OR REPLACE FUNCTION fn_validar_persona_activa(p_persona_id INTEGER)
    RETURNS VOID AS $$
DECLARE
    v_activo BOOLEAN;
BEGIN
    SELECT activo INTO v_activo FROM persona WHERE id = p_persona_id;

    IF v_activo IS NULL THEN
        RAISE EXCEPTION 'La persona % no existe', p_persona_id;
    END IF;

    IF NOT v_activo THEN
        RAISE EXCEPTION 'La persona % está desactivada', p_persona_id
            USING ERRCODE = '75001';
    END IF;
END;
$$ LANGUAGE plpgsql STABLE;

-- fn_sesion_abierta_ahora: Permite usarse en fn_obtener_o_crear_sesion_del_momento para poder decidir si mandar la existente o crear una nueva
CREATE OR REPLACE FUNCTION fn_sesion_abierta_ahora()
    RETURNS INTEGER AS $$
DECLARE
    v_sesion_id INTEGER;
    v_minutos_margen INTEGER;
BEGIN
    SELECT minutos_margen_ingreso INTO v_minutos_margen FROM configuracion_cola WHERE id = 1;

    SELECT id INTO v_sesion_id
    FROM sesion
    WHERE fecha = CURRENT_DATE
      AND NOT cancelada
      AND now() >= (hora_inicio - make_interval(mins => v_minutos_margen))
      AND (hora_fin IS NULL OR now() <= hora_fin)
    ORDER BY (now() >= hora_inicio) DESC, hora_inicio DESC
    LIMIT 1;

    RETURN v_sesion_id;
END;
$$ LANGUAGE plpgsql STABLE;

-- fn_horario_vigente_ahora: Permite usarse en fn_obtener_o_crear_sesion_del_momento para poder decidir si mandar la existente o crear una nueva
CREATE OR REPLACE FUNCTION fn_horario_vigente_ahora(
    OUT p_horario_id INTEGER, OUT p_hora_inicio TIME, OUT p_hora_fin TIME
) AS $$
DECLARE
    v_dia_semana INTEGER;
    v_minutos_margen INTEGER;
BEGIN
    v_dia_semana := EXTRACT(ISODOW FROM now())::INTEGER; -- 1=lunes .. 7=domingo
    SELECT minutos_margen_ingreso INTO v_minutos_margen FROM configuracion_cola WHERE id = 1;

    SELECT h.id, h.hora_inicio, h.hora_fin
    INTO p_horario_id, p_hora_inicio, p_hora_fin
    FROM horario h
    WHERE h.dia_semana = v_dia_semana
      AND h.activo = true
      AND now()::time BETWEEN (h.hora_inicio - make_interval(mins => v_minutos_margen)) AND h.hora_fin
      AND NOT EXISTS (
        SELECT 1 FROM horario_excepcion he
        WHERE he.horario_id = h.id AND he.fecha = CURRENT_DATE
    )
    ORDER BY (now()::time >= h.hora_inicio) DESC, h.hora_inicio DESC
    LIMIT 1;
END;
$$ LANGUAGE plpgsql STABLE;

-- fn_obtener_o_crear_sesion_del_momento: Si hay sesión abierta, la devuelve. Si no, busca horario y crea la sesión.
-- Si no hay horario vigente, SQLSTATE 75002.
-- Nota: alguien que llega después de hora_fin
CREATE OR REPLACE FUNCTION fn_obtener_o_crear_sesion_del_momento()
    RETURNS INTEGER AS $$
DECLARE
    v_sesion_id INTEGER;
    v_horario RECORD;
BEGIN
    v_sesion_id := fn_sesion_abierta_ahora();
    IF v_sesion_id IS NOT NULL THEN
        RETURN v_sesion_id;
    END IF;

    SELECT * INTO v_horario FROM fn_horario_vigente_ahora();

    IF v_horario.p_horario_id IS NULL THEN
        RAISE EXCEPTION 'No hay ninguna clase programada en este momento'
            USING ERRCODE = '75002';
    END IF;

    -- Si ya existe una sesión para este horario y fecha pero no se encontró arriba es porque su rango ya se cerró (ej. el admin la acortó a mano).
    -- No se crea una segunda, esto para evitar errores en la persistencia.
    IF EXISTS (SELECT 1 FROM sesion WHERE horario_id = v_horario.p_horario_id AND fecha = CURRENT_DATE) THEN
        RAISE EXCEPTION 'No hay ninguna clase programada en este momento'
            USING ERRCODE = '75002';
    END IF;

    INSERT INTO sesion (horario_id, fecha, hora_inicio, hora_fin)
    VALUES (
               v_horario.p_horario_id,
               CURRENT_DATE,
               (CURRENT_DATE + v_horario.p_hora_inicio)::timestamptz,
               (CURRENT_DATE + v_horario.p_hora_fin)::timestamptz
           )
    RETURNING id INTO v_sesion_id;

    RETURN v_sesion_id;
END;
$$ LANGUAGE plpgsql;
