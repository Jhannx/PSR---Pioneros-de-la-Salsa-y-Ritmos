-- PSR Aeróbicos — Vistas

-- vista_saldo_persona --
CREATE OR REPLACE VIEW vista_saldo_persona AS
SELECT
    p.id AS persona_id, p.nombre, p.apellidos,
    COALESCE(m.total_abonado, 0) AS total_abonado,
    COALESCE(a.total_cobrado, 0) AS total_cobrado,
    COALESCE(m.total_abonado, 0) - COALESCE(a.total_cobrado, 0) AS saldo
FROM persona p
         LEFT JOIN (
    SELECT persona_id, SUM(monto_abonado) AS total_abonado
    FROM movimiento_cuenta
    GROUP BY persona_id
) m ON m.persona_id = p.id
         LEFT JOIN (
    SELECT l.persona_id, SUM(a.monto_cobrado) AS total_cobrado
    FROM asistencia a
             JOIN llegada l ON l.id = a.llegada_id
    GROUP BY l.persona_id
) a ON a.persona_id = p.id
WHERE p.activo = true;

-- vista_fila_hoy --
-- Panel admin. Posición contada por sesión
CREATE OR REPLACE VIEW vista_fila_hoy AS
SELECT
    l.id AS llegada_id, l.sesion_id, l.persona_id,
    p.nombre, p.apellidos,
    l.hora_llegada, l.metodo_checkin,
    ROW_NUMBER() OVER (PARTITION BY l.sesion_id ORDER BY l.hora_llegada) AS posicion,
    a.id AS asistencia_id, a.monto_cobrado, a.hora_marcado,
    (a.id IS NOT NULL) AS atendido,
    sp.saldo, m.nota
FROM llegada l
         JOIN sesion s ON s.id = l.sesion_id
         JOIN persona p ON p.id = l.persona_id
         LEFT JOIN asistencia a ON a.llegada_id = l.id
         LEFT JOIN vista_saldo_persona sp ON sp.persona_id = l.persona_id
         LEFT JOIN LATERAL (
    SELECT nota
    FROM movimiento_cuenta
    WHERE llegada_id = l.id
    ORDER BY fecha_hora DESC
    LIMIT 1
    ) m ON true
WHERE s.fecha = CURRENT_DATE AND NOT s.cancelada
ORDER BY l.hora_llegada;

-- vista_fila_prepagada -- Alumnos en fila que ya pagaron lo suficiente para esta sesión — pueden entrar directo, sin esperar confirmación del admin.
CREATE OR REPLACE VIEW vista_fila_prepagada AS
SELECT
    l.id AS llegada_id, p.nombre, p.apellidos, l.hora_llegada
FROM llegada l
         JOIN sesion s ON s.id = l.sesion_id
         JOIN persona p ON p.id = l.persona_id
         LEFT JOIN asistencia a ON a.llegada_id = l.id
WHERE s.fecha = CURRENT_DATE
  AND NOT s.cancelada
  AND a.id IS NULL
  AND now() <= l.hora_llegada + make_interval(mins => (SELECT minutos_visibilidad_pagos FROM configuracion_cola WHERE id = 1))
  AND (
          COALESCE((SELECT SUM(m.monto_abonado) FROM movimiento_cuenta m WHERE m.persona_id = l.persona_id AND m.fecha_hora < l.hora_llegada), 0)
              -
          COALESCE((SELECT SUM(a2.monto_cobrado) FROM asistencia a2 JOIN llegada l2 ON l2.id = a2.llegada_id WHERE l2.persona_id = l.persona_id AND a2.hora_marcado < l.hora_llegada), 0)
          ) >= fn_tarifa_vigente()
ORDER BY l.hora_llegada;

-- vista_fila_espera --
-- Pantalla TV. Solo llegadas de hoy sin asistencias, dinero ni datos que solo debe de conocer el admin.
CREATE OR REPLACE VIEW vista_fila_espera AS
SELECT
    l.id AS llegada_id, l.sesion_id,
    p.nombre, p.apellidos, l.hora_llegada,
    ROW_NUMBER() OVER (PARTITION BY l.sesion_id ORDER BY l.hora_llegada) AS posicion
FROM llegada l
         JOIN sesion s ON s.id = l.sesion_id
         JOIN persona p ON p.id = l.persona_id
         LEFT JOIN asistencia a ON a.llegada_id = l.id
WHERE s.fecha = CURRENT_DATE
  AND NOT s.cancelada
  AND a.id IS NULL
  AND NOT EXISTS (SELECT 1 FROM vista_fila_prepagada vp WHERE vp.llegada_id = l.id)
ORDER BY l.hora_llegada;

-- vista_vueltos_pendientes --
CREATE OR REPLACE VIEW vista_vueltos_pendientes AS
SELECT
    m.id AS movimiento_id, m.persona_id,
    COALESCE(p.nombre || ' ' || p.apellidos, 'Persona eliminada') AS nombre_completo,
    m.monto_recibido, m.monto_abonado,
    (m.monto_recibido - m.monto_abonado) AS vuelto,
    m.fecha_hora
FROM movimiento_cuenta m
         JOIN persona p ON p.id = m.persona_id
WHERE m.vuelto_entregado = false
  AND m.tipo_movimiento = 'ABONO'
  AND m.monto_recibido > m.monto_abonado;

-- vista_recaudo_diario --
CREATE OR REPLACE VIEW vista_recaudo_diario AS
SELECT
    DATE(m.fecha_hora) AS fecha, s.id AS sesion_id,
    SUM(m.monto_abonado) AS total_recaudado
FROM movimiento_cuenta m
         LEFT JOIN llegada l ON l.id = m.llegada_id
         LEFT JOIN sesion s ON s.id = l.sesion_id
GROUP BY DATE(m.fecha_hora), s.id
ORDER BY fecha DESC, sesion_id;

-- vista_recaudo_por_sesion --
-- Recaudo por sesión, para cuando un día tiene más de una.
CREATE OR REPLACE VIEW vista_recaudo_por_sesion AS
SELECT
    s.fecha, s.id AS sesion_id,
    SUM(m.monto_abonado) AS total_recaudado
FROM movimiento_cuenta m
         JOIN llegada l ON l.id = m.llegada_id
         JOIN sesion s ON s.id = l.sesion_id
GROUP BY s.fecha, s.id
ORDER BY s.fecha DESC, s.id;

-- vista_saldo_por_asistencia -- Por cada asistencia cobrada, solo si el saldo acumulado de la persona la cubre.
CREATE OR REPLACE VIEW vista_saldo_por_asistencia AS
WITH asistencias_ordenadas AS (
    SELECT
        a.id AS asistencia_id, l.persona_id,
        s.fecha, a.monto_cobrado,
        SUM(a.monto_cobrado) OVER (
            PARTITION BY l.persona_id
            ORDER BY s.fecha, a.id
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
            ) AS acumulado_cobrado_hasta_aqui
    FROM asistencia a
             JOIN llegada l ON l.id = a.llegada_id
             JOIN sesion s ON s.id = l.sesion_id
),
     totales AS (
         SELECT persona_id, SUM(monto_abonado) AS total_abonado
         FROM movimiento_cuenta
         GROUP BY persona_id
     )
SELECT
    ao.asistencia_id, ao.persona_id, ao.fecha, ao.monto_cobrado,
    (COALESCE(t.total_abonado, 0) - ao.acumulado_cobrado_hasta_aqui) AS saldo_despues_de_esta_asistencia,
    CASE
        WHEN (COALESCE(t.total_abonado, 0) - ao.acumulado_cobrado_hasta_aqui) >= 0
            THEN 'CUBIERTA'
        WHEN (COALESCE(t.total_abonado, 0) - ao.acumulado_cobrado_hasta_aqui) > -ao.monto_cobrado
            THEN 'PARCIAL'
        ELSE 'DEBE'
        END AS estado_saldo,
    GREATEST(
            0,
            ao.monto_cobrado - GREATEST(0, COALESCE(t.total_abonado, 0) - ao.acumulado_cobrado_hasta_aqui + ao.monto_cobrado)
    ) AS monto_pendiente
FROM asistencias_ordenadas ao
         LEFT JOIN totales t ON t.persona_id = ao.persona_id
ORDER BY ao.persona_id, ao.fecha;

-- vista_historial_asistencias --
CREATE OR REPLACE VIEW vista_historial_asistencias AS
SELECT
    a.id AS asistencia_id, l.id AS llegada_id, s.id AS sesion_id, l.persona_id,
    COALESCE(p.nombre || ' ' || p.apellidos, 'Persona eliminada') AS nombre_completo,
    s.fecha, l.hora_llegada, l.metodo_checkin,
    a.monto_cobrado, a.hora_marcado, a.marcado_por,
    COALESCE(admin_persona.nombre || ' ' || admin_persona.apellidos,
             admin_usuario.numero_documento,
             admin_usuario.correo) AS marcado_por_nombre,
    mp.nombre AS metodo_pago
FROM asistencia a
         JOIN llegada l ON l.id = a.llegada_id
         JOIN sesion s ON s.id = l.sesion_id
         JOIN persona p ON p.id = l.persona_id
         LEFT JOIN usuario admin_usuario ON admin_usuario.id = a.marcado_por
         LEFT JOIN persona admin_persona ON admin_persona.id = admin_usuario.persona_id
         LEFT JOIN LATERAL (
    SELECT metodo_pago_id
    FROM movimiento_cuenta
    WHERE llegada_id = l.id
    ORDER BY fecha_hora DESC
    LIMIT 1
    ) m ON true
         LEFT JOIN metodo_pago mp ON mp.id = m.metodo_pago_id
ORDER BY s.fecha DESC, l.hora_llegada DESC;

-- vista_historial_cambios --
CREATE OR REPLACE VIEW vista_historial_cambios AS
SELECT
    h.id, h.tabla_afectada, h.registro_id,
    h.campo, h.valor_anterior, h.valor_nuevo,
    u.id AS usuario_id,
    COALESCE(p.nombre || ' ' || p.apellidos, u.numero_documento, u.correo, 'Persona eliminada') AS realizado_por,
    h.fecha_hora
FROM historial_cambio h
         LEFT JOIN usuario u ON u.id = h.usuario_id
         LEFT JOIN persona p ON p.id = u.persona_id
ORDER BY h.fecha_hora DESC;

-- vista_perfil_persona --
CREATE OR REPLACE VIEW vista_perfil_persona AS
SELECT
    p.id AS persona_id,
    p.nombre || ' ' || p.apellidos AS nombre_completo,
    p.tipo_documento, p.numero_documento, p.telefono, p.activo,
    ce.nombre AS contacto_emergencia_nombre, ce.telefono AS contacto_emergencia_telefono,
    ce.observaciones AS observaciones_emergencia,
    sp.saldo,
    fn_dias_credito_disponible(p.id) AS dias_credito_disponible,
    COALESCE(res.total_asistencias, 0) AS total_asistencias,
    res.ultima_asistencia_fecha, res.ultima_asistencia_hora
FROM persona p
         LEFT JOIN contacto_emergencia ce ON ce.persona_id = p.id
         LEFT JOIN vista_saldo_persona sp ON sp.persona_id = p.id
         LEFT JOIN (
    SELECT l.persona_id,
           COUNT(a.id) AS total_asistencias,
           MAX(s.fecha) AS ultima_asistencia_fecha,
           MAX(a.hora_marcado) AS ultima_asistencia_hora
    FROM llegada l
             JOIN sesion s ON s.id = l.sesion_id
             JOIN asistencia a ON a.llegada_id = l.id
    GROUP BY l.persona_id
) res ON res.persona_id = p.id;

-- vista_resumen_sesiones -- Una fila por sesión, muestra llegadas y asistencias confirmadas.
CREATE OR REPLACE VIEW vista_resumen_sesiones AS
SELECT
    s.id AS sesion_id, s.horario_id,
    s.fecha, s.hora_inicio, s.hora_fin,
    s.profesor_usuario_id,
    COALESCE(pp.nombre || ' ' || pp.apellidos, pu.correo, 'Sin asignar') AS profesor_nombre,
    s.cancelada,
    COUNT(DISTINCT l.id) AS total_llegadas,
    COUNT(DISTINCT a.id) AS total_atendidos
FROM sesion s
         LEFT JOIN llegada l ON l.sesion_id = s.id
         LEFT JOIN asistencia a ON a.llegada_id = l.id
         LEFT JOIN usuario pu ON pu.id = s.profesor_usuario_id
         LEFT JOIN persona pp ON pp.id = pu.persona_id
GROUP BY s.id, s.horario_id, s.fecha, s.hora_inicio, s.hora_fin,
         s.profesor_usuario_id, pp.nombre, pp.apellidos, pu.correo, s.cancelada
ORDER BY s.fecha DESC, s.hora_inicio DESC;
