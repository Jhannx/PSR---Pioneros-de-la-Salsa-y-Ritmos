package com.jhanp.psraerobicsbackend.modules.aerobics.session.entity;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.Schedule;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "sesion")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_id")
    private Schedule schedule;

    @Column(name = "fecha", nullable = false)
    private LocalDate date;

    @Column(name = "hora_inicio", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "hora_fin")
    private OffsetDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_usuario_id")
    private User teacher;

    @Column(name = "cancelada", nullable = false)
    private Boolean cancelled;

    /**
     * En que momento esta la sesion ahora mismo: UPCOMING, IN_PROGRESS, ENDED o CANCELLED.
     * <p>
     * <b>@Formula y no un calculo en Java, a proposito.</b> La regla ya vive en fn_estado_sesion, que
     * es la que usa vista_fila_hoy; reescribirla aqui dejaria dos definiciones de lo mismo que pueden
     * separarse con el tiempo — es exactamente el error que se cometio con el saldo reembolsable.
     * Hibernate mete la llamada en el SELECT de cada fila, asi que no hay una consulta por sesion.
     * <p>
     * De solo lectura: no existe como columna y nunca se escribe.
     */
    @org.hibernate.annotations.Formula("fn_estado_sesion(id)")
    private String status;

    /**
     * El nombre de quien dicta, ya resuelto.
     * <p>
     * Con @Formula y no navegando la relacion porque {@code teacher} es LAZY: leerlo por ahi dispara
     * una consulta por sesion, y la pantalla de Sesiones pide veinte de golpe. Esa es la razon de que
     * el nombre del profesor "apareciera con retraso" — el frontend estaba cruzando tres respuestas
     * para componerlo.
     * <p>
     * COALESCE en cascada, como en las vistas: una cuenta tecnica puede no tener persona, y entonces
     * lo unico que la identifica es el correo.
     */
    @org.hibernate.annotations.Formula("""
            (SELECT COALESCE(pp.nombre || ' ' || pp.apellidos, pu.correo)
             FROM usuario pu LEFT JOIN persona pp ON pp.id = pu.persona_id
             WHERE pu.id = profesor_usuario_id)
            """)
    private String teacherName;

    /** Cuantas personas llegaron. Subconsulta en el SELECT: no es una consulta por sesion. */
    @org.hibernate.annotations.Formula("(SELECT COUNT(*) FROM llegada l WHERE l.sesion_id = id)")
    private Long totalCheckins;

    /** De esas, a cuantas se les confirmo la asistencia. */
    @org.hibernate.annotations.Formula("""
            (SELECT COUNT(*) FROM asistencia a
             JOIN llegada l ON l.id = a.llegada_id
             WHERE l.sesion_id = id)
            """)
    private Long totalAttended;

    @PrePersist
    protected void onCreate() {
        if (this.cancelled == null) {
            this.cancelled = false;
        }
    }
}
