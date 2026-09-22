package com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SessionResponse(
        Integer id,
        Integer scheduleId,
        LocalDate date,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Integer teacherUserId,
        Boolean cancelled,

        /**
         * {@code UPCOMING}, {@code IN_PROGRESS}, {@code ENDED} o {@code CANCELLED}, con el reloj del
         * servidor.
         * <p>
         * Lo calcula fn_estado_sesion, la misma funcion que alimenta la cola. El frontend lo estaba
         * deduciendo comparando horas en el cliente: ademas de repetir la regla en cada pantalla, el
         * reloj del navegador no es el del servidor y dos personas podian ver estados distintos de la
         * misma clase.
         */
        String status,

        /** Quien dicta, ya resuelto: la pantalla no tiene que cruzar respuestas para componerlo. */
        String teacherName,

        Long totalCheckins,
        Long totalAttended,

        /**
         * Si <b>quien pregunta</b> puede finalizar esta sesion. Depende del rol y de si la sesion es
         * suya, asi que lo decide el servidor: sin esto, a un profesor se le ofrecia "Finalizar" en
         * una clase ajena para darle un 403 despues.
         */
        Boolean canClose,

        /** Si quien pregunta puede cancelarla. */
        Boolean canCancel
) {
}
