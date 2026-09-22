package com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.AccountMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountMovementRepository extends JpaRepository<AccountMovement, Integer> {

    Page<AccountMovement> findByPersonIdOrderByTimestampDesc(Integer personId, Pageable pageable);

    List<AccountMovement> findByCheckinId(Integer checkinId);

    List<AccountMovement> findByTimestampBetweenOrderByTimestampDesc(OffsetDateTime from, OffsetDateTime to);

    /**
     * Si un abono ya tiene algún reembolso. Decía "evita emitir dos reembolsos contra el mismo abono"
     * y eso era falso: el método no se llama desde ningún sitio, así que no evitaba nada. Un abono
     * admite varios reembolsos parciales, y desde el 13-sep es el caso normal —
     * {@code POST /api/personas/{id}/reembolso} reparte un monto entre varios abonos. Lo que controla
     * que no se devuelva de más es el tope por saldo restante de sp_registrar_reembolso.
     */
    boolean existsByOriginalMovementId(Integer originalMovementId);

    /**
     * El reembolso que acaba de emitir sp_registrar_reembolso contra un abono. El procedimiento no
     * devuelve el id, así que hay que ir a buscarlo (patrón H).
     * <p>
     * Existe para no traer la tabla entera a memoria y filtrarla en Java, que es como estaba resuelto:
     * eso crecía con el histórico de movimientos y acababa costando un reembolso más caro cada mes.
     * Pedir el más reciente no es una formalidad: con reembolsos parciales hay varios por abono.
     */
    Optional<AccountMovement> findTopByOriginalMovementIdOrderByTimestampDesc(Integer originalMovementId);

    /**
     * Abonos de una persona que todavía tienen saldo por devolver, del más antiguo al más reciente,
     * con cuánto queda en cada uno.
     * <p>
     * El {@code disponible} es el abono menos lo ya reembolsado contra él (los reembolsos guardan el
     * monto en negativo, de ahí el signo cambiado). El {@code HAVING} descarta los que ya están
     * devueltos del todo, para que quien reparta el monto no tenga que filtrarlos.
     * <p>
     * El orden es FIFO —el abono más antiguo primero— y es una decisión de negocio, no un detalle:
     * determina contra qué movimiento queda registrada la devolución. Se elige FIFO porque es el
     * criterio que una persona de mostrador puede explicar en voz alta: se devuelve empezando por lo
     * que se pagó primero.
     * <p>
     * Nativa porque {@code tipo_movimiento} es un enum de Postgres y JPQL no sabe compararlo con un
     * literal — el mismo motivo que en UserRepository.
     */
    @Query(value = """
            SELECT m.id AS movementId,
                   m.monto_abonado - COALESCE((
                       SELECT SUM(-r.monto_abonado)
                       FROM movimiento_cuenta r
                       WHERE r.movimiento_original_id = m.id
                         AND r.tipo_movimiento = 'REEMBOLSO'
                   ), 0) AS available
            FROM movimiento_cuenta m
            WHERE m.persona_id = :personId
              AND m.tipo_movimiento = 'ABONO'
            GROUP BY m.id, m.monto_abonado, m.fecha_hora
            HAVING m.monto_abonado - COALESCE((
                       SELECT SUM(-r.monto_abonado)
                       FROM movimiento_cuenta r
                       WHERE r.movimiento_original_id = m.id
                         AND r.tipo_movimiento = 'REEMBOLSO'
                   ), 0) > 0
            ORDER BY m.fecha_hora ASC, m.id ASC
            """, nativeQuery = true)
    List<RefundableMovement> findRefundableByPerson(@Param("personId") Integer personId);

    /**
     * El tope real de un reembolso para esta persona: lo abonado que sigue sin devolverse, <b>acotado
     * al saldo a favor</b>.
     * <p>
     * No es lo mismo que sumar los {@code available} de {@link #findRefundableByPerson}, y esa
     * diferencia fue un fallo de dinero: esa suma no descuenta lo que la persona ya consumió en
     * asistencias, así que quien abonó 15.000 y usó 10.000 aparecía con 15.000 reembolsables y, al
     * devolvérselos, la cuenta quedaba debiendo. Los dos métodos siguen conviviendo porque responden
     * a preguntas distintas: aquel dice <em>contra qué abonos</em> se reparte, este dice
     * <em>cuánto</em> como máximo.
     * <p>
     * Delega en {@code fn_saldo_reembolsable}, que es la misma función que usa la columna
     * {@code reembolsable} de vista_saldo_persona. No se reescribe la fórmula aquí a propósito: el
     * número que se muestra y el que se valida tienen que ser el mismo por construcción, no por
     * disciplina.
     */
    @Query(value = "SELECT fn_saldo_reembolsable(:personId)", nativeQuery = true)
    BigDecimal findRefundableTotal(@Param("personId") Integer personId);

    /**
     * Lo recaudado hoy: la suma de lo ABONADO, neto de reembolsos.
     * <p>
     * <b>Lo abonado y no lo recibido, y es una decision anotada.</b> Quien paga una clase de 5.000 con
     * un billete de 50.000 deja 50.000 en el cajon, pero solo 5.000 son recaudo: los otros 45.000
     * siguen siendo suyos hasta que se le devuelvan o los abone. Contar lo recibido haria que un
     * billete grande multiplicara la cifra del dia por diez.
     * <p>
     * Los reembolsos restan solos: guardan el monto en negativo. Devolver dinero baja el recaudo,
     * que es lo correcto.
     * <p>
     * Se agrupa por la fecha del movimiento — cuando entro la plata— y no por la fecha de la clase.
     * Es la cifra de caja del dia, no el ingreso devengado: para eso esta el dashboard.
     */
    @Query(value = """
            SELECT COALESCE(SUM(m.monto_abonado), 0)
            FROM movimiento_cuenta m
            WHERE m.fecha_hora >= CURRENT_DATE
              AND m.fecha_hora < CURRENT_DATE + 1
            """, nativeQuery = true)
    BigDecimal findCollectedToday();

    /** Proyección de {@link #findRefundableByPerson}. */
    interface RefundableMovement {
        Integer getMovementId();

        BigDecimal getAvailable();
    }
}
