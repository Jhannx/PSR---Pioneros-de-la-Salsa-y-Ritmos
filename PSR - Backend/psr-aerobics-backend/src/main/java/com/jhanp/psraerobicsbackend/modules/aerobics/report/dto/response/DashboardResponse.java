package com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Salida de GET /api/reportes/dashboard: las métricas ya calculadas, para que el front solo las pinte.
 * <p>
 * Los ingresos son <b>devengados</b>, no caja: salen de vista_ingreso_diario, que atribuye cada cobro
 * a la fecha de la clase que se dictó y no a la fecha en que entró el dinero. Para la caja del día
 * está /api/reportes/recaudo-diario, que responde otra pregunta.
 */
public record DashboardResponse(

        /**
         * Los cuatro periodos de una sola vez, para que el filtro del front cambie al instante sin
         * volver a llamar al servidor. Son cuatro agregaciones baratas sobre la misma vista.
         */
        PeriodTotals totals,

        /**
         * Asistencias de un día de clase promedio dentro de la ventana de 30 días.
         * <p>
         * <b>No sigue al filtro de periodo:</b> siempre es un promedio POR DÍA, para que sea
         * comparable con {@code totals.day()}. Si el usuario mira el total del mes, este número sigue
         * respondiendo "cuánta gente viene un día normal".
         * <p>
         * El divisor son los días que tuvieron sesión, no los días de calendario: si el gimnasio abre
         * de lunes a jueves, dividir entre 30 daría un promedio artificialmente bajo.
         */
        Long averageAttendees,

        /**
         * Ingreso de un día de clase promedio. Mismo divisor y misma independencia del filtro.
         * <p>
         * Redondeado a pesos enteros: en COP el centavo no circula, y un promedio de 172.333,33 no
         * significa nada. Es la única cifra del sistema que sale de una división.
         */
        BigDecimal averageRevenue,

        /**
         * El método de pago más usado de todo el histórico, por CANTIDAD de movimientos —no por monto
         * acumulado, que daría otra respuesta si las transferencias son grandes y el efectivo menudo.
         * Null si todavía no hay ningún movimiento registrado.
         */
        String mainPaymentMethod,

        /** Siempre los siete días, de lunes a domingo, aunque alguno venga en cero. */
        List<WeeklyAttendanceAverage> weeklyAttendanceAverage
) {

    /**
     * Asistencias e ingreso de un periodo. Las dos cifras salen de las mismas filas, así que siempre
     * cuadran entre sí.
     */
    public record Totals(Long attendees, BigDecimal revenue) {
    }

    /**
     * Los cuatro periodos de calendario que contienen la fecha consultada, no ventanas móviles:
     * la semana es de lunes a domingo, el mes va del 1 al último día, el año del 1 de enero al 31 de
     * diciembre. Es lo que se entiende por "cuánto llevamos este mes" y lo que permite comparar contra
     * el mes anterior.
     */
    public record PeriodTotals(Totals day, Totals week, Totals month, Totals year) {
    }

    /**
     * Cuánta gente viene un lunes cualquiera, un martes cualquiera, etc.
     * <p>
     * El promedio se saca sobre las OCURRENCIAS de ese día dentro de la ventana de 30 días —los cuatro
     * lunes—, no sobre los 30 días. Dividir entre 30 mezclaría lunes con domingos y no respondería la
     * pregunta que hace el gráfico.
     */
    public record WeeklyAttendanceAverage(

            /** ISO: 1 = lunes … 7 = domingo. La etiqueta la pone el front. */
            Integer dayOfWeek,

            /** Cero si ese día nunca hubo clase en la ventana. */
            Long averageCount
    ) {
    }
}
