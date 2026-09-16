package com.jhanp.psraerobicsbackend.modules.aerobics.report.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Agregados del dashboard. Va con JdbcTemplate y no con Spring Data por dos razones: no hay entidad
 * detrás —todo sale de vista_ingreso_diario— y las ventanas son relativas a la fecha que llega por
 * parámetro, así que no encajan en una vista sin argumentos.
 * <p>
 * Mismo patrón que {@link com.jhanp.psraerobicsbackend.modules.person.repository.PersonProcedureRepository}.
 */
@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Asistencias e ingreso devengado de un rango, más los días que tuvieron clase dentro de él.
     * <p>
     * Un solo record para todo: los cuatro periodos del filtro solo necesitan las dos primeras cifras
     * e ignoran la tercera, que es el divisor de los promedios. Separarlos en dos consultas casi
     * idénticas no aportaba nada.
     */
    public record RangeTotals(long attendees, BigDecimal revenue, long daysWithSession) {}

    /**
     * Rango inclusivo en ambos extremos. Para un solo día se llama con la misma fecha dos veces.
     * <p>
     * COUNT(DISTINCT fecha) y no COUNT(*): un día con dos sesiones sigue siendo un solo día de clase.
     * Contar filas inflaría el divisor y hundiría el promedio justo los días de más actividad.
     */
    public RangeTotals findRangeTotals(LocalDate from, LocalDate to) {
        String sql = """
                SELECT COALESCE(SUM(total_asistencias), 0) AS asistencias,
                       COALESCE(SUM(total_ingreso), 0)     AS ingreso,
                       COUNT(DISTINCT fecha)               AS dias_con_sesion
                FROM vista_ingreso_diario
                WHERE fecha BETWEEN ? AND ?
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new RangeTotals(
                rs.getLong("asistencias"),
                rs.getBigDecimal("ingreso"),
                rs.getLong("dias_con_sesion")), from, to);
    }

    /**
     * El método de pago más usado de todo el histórico, por cantidad de movimientos.
     * <p>
     * El desempate por nombre no es cosmético: sin él, dos métodos con el mismo número de movimientos
     * podrían alternarse entre consultas y el dashboard parpadearía sin que nada hubiera cambiado.
     *
     * @return null si todavía no hay ningún movimiento registrado
     */
    public String findMainPaymentMethod() {
        String sql = """
                SELECT mp.nombre
                FROM movimiento_cuenta m
                         JOIN metodo_pago mp ON mp.id = m.metodo_pago_id
                GROUP BY mp.nombre
                ORDER BY COUNT(*) DESC, mp.nombre
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null);
    }

    /**
     * Asistencias promedio por día de la semana dentro de la ventana.
     * <p>
     * El divisor es el número de veces que ese día tuvo clase —los cuatro lunes de los últimos 30
     * días—, no la longitud de la ventana: dividir entre 30 mezclaría lunes con domingos.
     * <p>
     * Devuelve solo los días que aparecen en los datos. Completar los siete es tarea del service, que
     * es quien conoce el contrato de salida.
     *
     * @return día ISO (1 = lunes … 7 = domingo) → promedio redondeado
     */
    public Map<Integer, Long> findWeeklyAttendanceAverage(LocalDate from, LocalDate to) {
        String sql = """
                SELECT EXTRACT(ISODOW FROM fecha)::int AS dia,
                       SUM(total_asistencias)          AS total,
                       COUNT(DISTINCT fecha)           AS ocurrencias
                FROM vista_ingreso_diario
                WHERE fecha BETWEEN ? AND ?
                GROUP BY EXTRACT(ISODOW FROM fecha)
                """;

        Map<Integer, Long> averageByDay = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            long occurrences = rs.getLong("ocurrencias");
            // No puede ser 0 —el GROUP BY solo devuelve días presentes— pero dividir a ciegas por un
            // valor que viene de la base es la clase de descuido que revienta en producción.
            long average = occurrences == 0
                    ? 0
                    : Math.round((double) rs.getLong("total") / occurrences);

            averageByDay.put(rs.getInt("dia"), average);
        }, from, to);

        return averageByDay;
    }
}
