package com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.entity.Checkin;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.enums.MovementType;
import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * @DynamicUpdate por lo mismo que en {@code Person}: tipo_movimiento es un enum de Postgres que el
 * converter bindea como varchar, y un UPDATE con todas las columnas lo arrastraba aunque la
 * corrección de un movimiento solo cambie montos, método de pago o nota. Con el UPDATE limitado a lo
 * modificado, la columna enum no entra en la sentencia. El alta de movimientos no sufre esto: va por
 * sp_registrar_movimiento, que ya pasa el enum como Types.OTHER.
 */
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@DynamicUpdate
@Table(name = "movimiento_cuenta")
public class AccountMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llegada_id")
    private Checkin checkin;

    @Column(name = "monto_recibido", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountReceived;

    @Column(name = "monto_abonado", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountCredited;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metodo_pago_id", nullable = false)
    private PaymentMethod paymentMethod;

    // Sin @Enumerated: MovementTypeConverter se aplica solo y guarda el valor en español.
    @Column(name = "tipo_movimiento", nullable = false, columnDefinition = "tipo_movimiento_enum")
    private MovementType movementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_original_id")
    private AccountMovement originalMovement;

    @Column(name = "vuelto_entregado", nullable = false)
    private Boolean changeDelivered;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private OffsetDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(name = "nota")
    private String note;

    @PrePersist
    protected void onCreate() {
        this.timestamp = OffsetDateTime.now();
        if (this.changeDelivered == null) {
            this.changeDelivered = false;
        }
    }
}
