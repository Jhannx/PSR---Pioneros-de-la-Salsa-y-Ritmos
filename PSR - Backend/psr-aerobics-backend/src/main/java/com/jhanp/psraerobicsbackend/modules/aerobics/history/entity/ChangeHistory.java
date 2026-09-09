package com.jhanp.psraerobicsbackend.modules.aerobics.history.entity;

import com.jhanp.psraerobicsbackend.modules.aerobics.history.enums.AuditableTable;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "historial_cambio")
public class ChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Sin @Enumerated: AuditableTableConverter se aplica solo y guarda el valor en español.
    @Column(name = "tabla_afectada", nullable = false, columnDefinition = "tabla_auditable_enum")
    private AuditableTable affectedTable;

    @Column(name = "registro_id", nullable = false)
    private Integer recordId;

    @Column(name = "campo", nullable = false, length = 50)
    private String fieldName;

    @Column(name = "valor_anterior")
    private String previousValue;

    @Column(name = "valor_nuevo")
    private String newValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private OffsetDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = OffsetDateTime.now();
    }
}
