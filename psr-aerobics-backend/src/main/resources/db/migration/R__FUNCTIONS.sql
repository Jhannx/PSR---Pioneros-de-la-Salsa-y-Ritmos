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

-- fn_saldo_reembolsable: cuánto se le puede DEVOLVER en efectivo a una persona.
--
-- Es el tope de un reembolso, y no es ni el saldo ni lo abonado. Son dos límites a la vez y manda el
-- menor:
--
--   1. Lo abonado que todavía no se ha devuelto. No se puede devolver dos veces el mismo abono.
--   2. El saldo a favor. No se puede devolver plata de una clase que sí se dio.
--
-- El segundo límite faltaba, y el fallo no era teórico: alguien que abonó 15.000 y consumió 10.000 en
-- asistencias tiene 5.000 a favor, pero el cálculo anterior ofrecía devolverle los 15.000 enteros —y
-- al hacerlo la cuenta quedaba debiendo 10.000. Se devolvía dinero de clases ya dictadas.
--
-- GREATEST(..., 0) al final: quien debe no tiene nada que recibir, y un tope negativo no significa
-- nada en el formulario de reembolso.
--
-- <b>Esta función es la ÚNICA definición de la fórmula.</b> La usan vista_saldo_persona (y por ella
-- vista_fila_hoy) y la validación de POST /api/personas/{id}/reembolso, que la llama por
-- AccountMovementRepository.findRefundableTotal. Antes la fórmula estaba escrita dos veces —una en la
-- vista y otra en el repositorio— y bastaba con corregir una sola para que el número que se muestra y
-- el que se valida dejaran de coincidir.
--
-- Calcula desde las tablas base y NO desde vista_saldo_persona, por dos razones: esa vista la usa a
-- ella (sería circular), y filtra por activo = true — a una persona que se da de baja hay que poder
-- devolverle lo suyo.
CREATE OR REPLACE FUNCTION fn_saldo_reembolsable(p_persona_id INTEGER)
    RETURNS NUMERIC AS $$
DECLARE
    v_sin_devolver NUMERIC;
    v_saldo        NUMERIC;
BEGIN
    -- Límite 1: por cada abono, lo que le queda vivo. Los ya devueltos del todo se descartan con el
    -- WHERE para que un abono sobre-reembolsado (que no debería existir) no reste de los demás.
    SELECT COALESCE(SUM(ab.disponible), 0) INTO v_sin_devolver
    FROM (
             SELECT mc.monto_abonado - COALESCE((
                        SELECT SUM(-rf.monto_abonado)
                        FROM movimiento_cuenta rf
                        WHERE rf.movimiento_original_id = mc.id
                          AND rf.tipo_movimiento = 'REEMBOLSO'
                    ), 0) AS disponible
             FROM movimiento_cuenta mc
             WHERE mc.persona_id = p_persona_id
               AND mc.tipo_movimiento = 'ABONO'
         ) ab
    WHERE ab.disponible > 0;

    -- Límite 2: el saldo, con la misma definición que vista_saldo_persona (los reembolsos ya van
    -- dentro de total_abonado, en negativo).
    SELECT COALESCE((SELECT SUM(m.monto_abonado)
                     FROM movimiento_cuenta m
                     WHERE m.persona_id = p_persona_id), 0)
               - COALESCE((SELECT SUM(a.monto_cobrado)
                           FROM asistencia a
                                    JOIN llegada l ON l.id = a.llegada_id
                           WHERE l.persona_id = p_persona_id), 0)
    INTO v_saldo;

    RETURN GREATEST(LEAST(v_sin_devolver, v_saldo), 0);
END;
$$ LANGUAGE plpgsql STABLE;

-- fn_vuelto_pendiente: cuanto dinero se le debe a una persona en vueltos que nunca se entregaron.
--
-- Es la tercera cifra de la cuenta y no se parece a las otras dos: el saldo es credito para clases
-- futuras, lo reembolsable es lo que se puede devolver de lo abonado, y esto es dinero que la persona
-- entrego de mas y que sigue en la caja. Un billete de 50.000 por una clase de 4.000 cuando no hay
-- con que dar el cambio deja 46.000 aqui.
--
-- Solo los ABONOs: un reembolso tiene monto_recibido = 0 por el CHECK de la tabla, asi que restaria.
CREATE OR REPLACE FUNCTION fn_vuelto_pendiente(p_persona_id INTEGER)
    RETURNS NUMERIC AS $$
DECLARE
    v_total NUMERIC;
BEGIN
    SELECT COALESCE(SUM(m.monto_recibido - m.monto_abonado), 0) INTO v_total
    FROM movimiento_cuenta m
    WHERE m.persona_id = p_persona_id
      AND m.tipo_movimiento = 'ABONO'
      AND m.vuelto_entregado = false
      AND m.monto_recibido > m.monto_abonado;

    RETURN v_total;
END;
$$ LANGUAGE plpgsql STABLE;

-- fn_estado_cuenta: como esta la cuenta de una persona, en una sola palabra.
--
-- Existe porque el frontend lo estaba deduciendo comparando el saldo con cero, y se equivocaba en un
-- caso corriente: quien pago 10.000, abono dos dias y consumio uno tiene un dia a favor y no se le
-- debe nada, pero aparecia en "saldo pendiente". El estado depende de DOS cifras (el saldo y el
-- vuelto sin entregar) y de cual manda cuando las dos son distintas de cero; esa regla es negocio, y
-- el negocio vive aqui.
--
-- El orden de las ramas ES la regla, y no es arbitrario:
--
--   DEBE             el saldo es negativo. Manda sobre todo lo demas: si la persona debe dinero, que
--                    ademas haya un vuelto sin entregar no cambia que lo primero es cobrarle.
--   VUELTO_PENDIENTE no debe, y hay dinero suyo en la caja que nunca se le devolvio.
--   AL_DIA           todo lo demas, INCLUIDO el saldo a favor por dias pagados por adelantado. Tener
--                    credito no es un pendiente: es exactamente lo contrario.
CREATE OR REPLACE FUNCTION fn_estado_cuenta(p_persona_id INTEGER)
    RETURNS TEXT AS $$
DECLARE
    v_saldo  NUMERIC;
    v_vuelto NUMERIC;
BEGIN
    SELECT COALESCE((SELECT SUM(m.monto_abonado)
                     FROM movimiento_cuenta m
                     WHERE m.persona_id = p_persona_id), 0)
               - COALESCE((SELECT SUM(a.monto_cobrado)
                           FROM asistencia a
                                    JOIN llegada l ON l.id = a.llegada_id
                           WHERE l.persona_id = p_persona_id), 0)
    INTO v_saldo;

    IF v_saldo < 0 THEN
        RETURN 'DEBE';
    END IF;

    v_vuelto := fn_vuelto_pendiente(p_persona_id);

    IF v_vuelto > 0 THEN
        RETURN 'VUELTO_PENDIENTE';
    END IF;

    RETURN 'AL_DIA';
END;
$$ LANGUAGE plpgsql STABLE;

-- fn_monto_a_pagar: cuanto tiene que pagar HOY esta persona, en esta llegada.
--
-- Es la cifra del cobro, y hasta ahora el frontend la estaba deduciendo por su cuenta. Con eso,
-- "lo que supera la tarifa" se trataba como vuelto, y salian tres cosas mal a la vez: a quien debia de
-- antes no se le saldaba la deuda con lo que entregaba, a quien tenia un dia pagado por adelantado se
-- le volvia a cobrar la sesion, y un vuelto pendiente se leia como deuda de la persona.
--
-- La regla, que es la del modelo de saldo y no una nueva:
--
--   * Si la asistencia de esta llegada YA se confirmo, la tarifa de hoy ya esta cobrada y forma parte
--     del saldo. Lo que falta por pagar es la deuda, si la hay: -saldo.
--   * Si NO se ha confirmado, hay que cubrir la tarifa de hoy ademas de lo que se deba: tarifa - saldo.
--
-- Un saldo a favor descuenta solo, porque entra con signo positivo en las dos ramas: quien tiene un
-- dia adelantado da 0. Y GREATEST(..., 0) porque a quien le sobra credito no se le cobra nada, no se
-- le "cobra negativo".
--
-- El dinero que se recibe no se imputa a mano contra la deuda y luego contra la clase: todo entra al
-- mismo saldo, y por eso el orden "primero lo viejo, despues lo de hoy" se cumple por construccion.
CREATE OR REPLACE FUNCTION fn_monto_a_pagar(p_persona_id INTEGER, p_llegada_id INTEGER)
    RETURNS NUMERIC AS $$
DECLARE
    v_saldo      NUMERIC;
    v_confirmada BOOLEAN;
BEGIN
    SELECT COALESCE((SELECT SUM(m.monto_abonado)
                     FROM movimiento_cuenta m
                     WHERE m.persona_id = p_persona_id), 0)
               - COALESCE((SELECT SUM(a.monto_cobrado)
                           FROM asistencia a
                                    JOIN llegada l ON l.id = a.llegada_id
                           WHERE l.persona_id = p_persona_id), 0)
    INTO v_saldo;

    v_confirmada := EXISTS (SELECT 1 FROM asistencia WHERE llegada_id = p_llegada_id);

    IF v_confirmada THEN
        RETURN GREATEST(-v_saldo, 0);
    END IF;

    RETURN GREATEST(fn_tarifa_vigente() - v_saldo, 0);
END;
$$ LANGUAGE plpgsql STABLE;

-- fn_estado_sesion: en que momento de su vida esta una sesion, ahora mismo.
--
-- El frontend lo estaba calculando comparando horas en el cliente. Ademas de repetir la regla en cada
-- pantalla, el reloj del navegador no es el del servidor: dos personas podian ver estados distintos
-- de la misma clase.
--
-- CANCELADA gana siempre, aunque su hora ya haya pasado: una clase cancelada no "termino", no se dio.
-- Una sesion sin hora_fin se considera en curso hasta que alguien la cierre o la cierre el scheduler.
CREATE OR REPLACE FUNCTION fn_estado_sesion(p_sesion_id INTEGER)
    RETURNS TEXT AS $$
DECLARE
    v_sesion RECORD;
BEGIN
    SELECT cancelada, hora_inicio, hora_fin INTO v_sesion
    FROM sesion WHERE id = p_sesion_id;

    IF v_sesion IS NULL THEN
        RETURN NULL;
    END IF;

    IF v_sesion.cancelada THEN
        RETURN 'CANCELLED';
    END IF;

    IF now() < v_sesion.hora_inicio THEN
        RETURN 'UPCOMING';
    END IF;

    IF v_sesion.hora_fin IS NOT NULL AND now() > v_sesion.hora_fin THEN
        RETURN 'ENDED';
    END IF;

    RETURN 'IN_PROGRESS';
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

-- fn_obtener_o_crear_sesion_del_momento: a que sesion entra quien acaba de llegar.
--
-- Orden de preferencia, y cada paso responde a un caso real:
--
--   1. La sesion abierta ahora mismo. El caso normal.
--   2. La proxima sesion de hoy, SI empieza dentro de la proxima hora. Regla del producto confirmada
--      el 20-sep: la clase de las 18:00 termino y son las 19:10, pero hay otra a las 20:00 — quien
--      llega ahora viene a esa, no a la que acaba de acabar. Antes se le sumaba a la sesion ya
--      terminada y se le cobraba una clase que no iba a recibir.
--   3. El horario vigente en este momento, creando la sesion. El camino de siempre.
--   4. Nada: SQLSTATE 75002, "fuera de horario".
--
-- p_permitir_fuera_horario es la valvula de escape del mostrador. Con true, en vez de rechazar se
-- entra a la ultima sesion de hoy que no este cancelada. Solo lo usa el check-in MANUAL, que hace
-- una persona que tiene delante a quien llega y ve el caso concreto; los canales automaticos —el
-- propio del miembro, el QR y el de documento de la terminal— no pueden saltarsela, porque ahi no hay
-- nadie juzgando nada.
--
-- Se reemplaza la version sin parametro en vez de sobrecargarla: con dos versiones, una llamada sin
-- argumentos queda ambigua y Postgres la rechaza.
DROP FUNCTION IF EXISTS fn_obtener_o_crear_sesion_del_momento();

CREATE OR REPLACE FUNCTION fn_obtener_o_crear_sesion_del_momento(
    p_permitir_fuera_horario BOOLEAN DEFAULT false
)
    RETURNS INTEGER AS $$
DECLARE
    v_sesion_id INTEGER;
    v_horario RECORD;
BEGIN
    v_sesion_id := fn_sesion_abierta_ahora();
    IF v_sesion_id IS NOT NULL THEN
        RETURN v_sesion_id;
    END IF;

    -- La proxima de hoy, si esta a menos de una hora. La ventana es corta a proposito: mas ancha
    -- convertiria "llego tardisimo a la clase de la tarde" en "entro a la de la noche".
    SELECT id INTO v_sesion_id
    FROM sesion
    WHERE fecha = CURRENT_DATE
      AND NOT cancelada
      AND hora_inicio > now()
      AND hora_inicio <= now() + INTERVAL '1 hour'
    ORDER BY hora_inicio
    LIMIT 1;

    IF v_sesion_id IS NOT NULL THEN
        RETURN v_sesion_id;
    END IF;

    SELECT * INTO v_horario FROM fn_horario_vigente_ahora();

    IF v_horario.p_horario_id IS NULL THEN
        IF p_permitir_fuera_horario THEN
            -- La ultima de hoy: es a la que pertenece la gente que sigue en la sala.
            SELECT id INTO v_sesion_id
            FROM sesion
            WHERE fecha = CURRENT_DATE AND NOT cancelada
            ORDER BY hora_inicio DESC
            LIMIT 1;

            IF v_sesion_id IS NOT NULL THEN
                RETURN v_sesion_id;
            END IF;
        END IF;

        RAISE EXCEPTION 'No hay ninguna clase programada en este momento'
            USING ERRCODE = '75002';
    END IF;

    -- Si ya existe una sesión para este horario y fecha pero no se encontró arriba es porque su rango ya se cerró (ej. el admin la acortó a mano).
    -- No se crea una segunda, esto para evitar errores en la persistencia.
    IF EXISTS (SELECT 1 FROM sesion WHERE horario_id = v_horario.p_horario_id AND fecha = CURRENT_DATE) THEN
        IF p_permitir_fuera_horario THEN
            SELECT id INTO v_sesion_id
            FROM sesion
            WHERE horario_id = v_horario.p_horario_id AND fecha = CURRENT_DATE AND NOT cancelada;

            IF v_sesion_id IS NOT NULL THEN
                RETURN v_sesion_id;
            END IF;
        END IF;

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
