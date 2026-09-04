package com.jhanp.psraerobicsbackend.modules.aerobics.history.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

/**
 * vista_historial_cambios: igual que historial_cambio pero con el nombre de quien hizo el cambio ya
 * resuelto en performedBy. Es a la vez la entidad de solo lectura y el DTO de salida de
 * GET /api/auditoria/historial-cambios.
 */
@Getter
@Entity
@Immutable
@Table(name = "vista_historial_cambios")
public class ChangeHistoryView {

    @Id
    private Integer id;

    @Column(name = "tabla_afectada")
    private String affectedTable;

    @Column(name = "registro_id")
    private Integer recordId;

    @Column(name = "campo")
    private String field;

    @Column(name = "valor_anterior")
    private String previousValue;

    @Column(name = "valor_nuevo")
    private String newValue;

    @Column(name = "usuario_id")
    private Integer userId;

    @Column(name = "realizado_por")
    private String performedBy;

    @Column(name = "fecha_hora")
    private OffsetDateTime dateTime;
}
