package com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response;

import java.math.BigDecimal;

/**
 * Salida de {@code GET /api/reportes/recaudo-hoy}.
 * <p>
 * Un objeto para un solo numero, y no el numero suelto, por lo mismo que el resto de respuestas del
 * proyecto: si manana hace falta acompanarlo de otra cifra —el efectivo en el cajon, por ejemplo— se
 * anade un campo sin romper a quien ya lo consume.
 */
public record CollectedTodayResponse(

        /**
         * Lo abonado hoy, neto de reembolsos. <b>No es el efectivo que entro</b>: un cobro de 5.000
         * pagado con 50.000 cuenta 5.000, porque los otros 45.000 siguen siendo de la persona hasta
         * que se le devuelvan o los abone.
         */
        BigDecimal collectedToday
) {
}
