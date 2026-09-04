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
