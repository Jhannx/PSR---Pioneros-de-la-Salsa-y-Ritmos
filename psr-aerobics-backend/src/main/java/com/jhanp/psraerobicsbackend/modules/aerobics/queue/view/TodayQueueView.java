package com.jhanp.psraerobicsbackend.modules.aerobics.queue.view;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Immutable
@Table(name = "vista_fila_hoy")
public class TodayQueueView {

    @Id
    @Column(name = "llegada_id")
    private Integer checkinId;

    @Column(name = "sesion_id")
    private Integer sessionId;

    @Column(name = "persona_id")
    private Integer personId;

    @Column(name = "nombre")
    private String firstName;

    @Column(name = "apellidos")
    private String lastName;

    @Column(name = "hora_llegada")
    private OffsetDateTime arrivalTime;

    /**
     * CheckinMethod y no String, y de eso depende que el check-in responda.
     * <p>
     * Declarado como String, esta columna llegaba con el valor crudo de la base ("DOCUMENTO") y
     * MapStruct, al mapear hacia CheckinResponse y TodayQueueViewResponse —que declaran el enum—,
     * generaba un {@code Enum.valueOf(CheckinMethod.class, "DOCUMENTO")}. Como las constantes están
     * en inglés (DOCUMENT), reventaba con "No enum constant ...CheckinMethod.DOCUMENTO".
     * <p>
     * El detalle que lo hacía difícil de ver: con QR funcionaba, porque ahí el nombre de la constante
     * y el valor de la base coinciden. Solo fallaba con DOCUMENTO y MANUAL.
     * <p>
     * Con el tipo correcto entra CheckinMethodConverter, que sabe traducir, y MapStruct pasa a mapear
     * enum → enum. El converter es de JPA: sobre un String no se aplica nunca.
     */
    @Column(name = "metodo_checkin", columnDefinition = "metodo_checkin_enum")
    private CheckinMethod checkinMethod;

    @Column(name = "posicion")
    private Long position;

    @Column(name = "asistencia_id")
    private Integer attendanceId;

    @Column(name = "monto_cobrado")
    private BigDecimal amountCharged;

    @Column(name = "hora_marcado")
    private OffsetDateTime markedAt;

    @Column(name = "atendido")
    private Boolean attended;

    @Column(name = "saldo")
    private BigDecimal balance;

    @Column(name = "nota")
    private String note;

    /**
     * Cuánto se le puede devolver, que no es el saldo: aquí no se descuenta lo consumido en
     * asistencias. Ver el comentario de vista_saldo_persona.
     */
    @Column(name = "reembolsable")
    private BigDecimal refundable;

    /** Dinero suyo que sigue en la caja: lo recibido de mas en cobros cuyo vuelto nunca se entrego. */
    @Column(name = "vuelto_pendiente")
    private BigDecimal pendingChange;

    /** DEBE, VUELTO_PENDIENTE o AL_DIA. Lo decide fn_estado_cuenta, no el cliente. */
    @Column(name = "estado_cuenta")
    private String accountStatus;

    /** Lo recibido en ESTA llegada, sumando sus movimientos. Es la senal de "aqui ya se cobro". */
    @Column(name = "monto_recibido")
    private BigDecimal receivedAmount;

    /** Lo aplicado a la cuenta en esta llegada; los reembolsos ya restan aqui. */
    @Column(name = "monto_aplicado")
    private BigDecimal appliedAmount;

    /** UPCOMING, IN_PROGRESS, ENDED o CANCELLED, con el reloj del servidor. */
    @Column(name = "estado_sesion")
    private String sessionStatus;

    /** Lo que hay que cobrarle hoy, ya descontada su deuda anterior y su saldo a favor. */
    @Column(name = "monto_a_pagar")
    private BigDecimal amountDue;

    /** La sesion de hoy ya esta pagada con dias adelantados: no hay que cobrar nada. */
    @Column(name = "cubierto_por_saldo")
    private Boolean coveredByCredit;
}
