package com.jhanp.psraerobicsbackend.modules.consent.entity;

import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentMedium;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Una fila = un hecho fechado: esta persona otorgó (o revocó) este consentimiento, este día, por este
 * medio, sobre esta versión de la política.
 * <p>
 * <b>Esta entidad solo se inserta.</b> No hay setters que se usen después del alta, no hay
 * {@code @PreUpdate}, y ningún service la modifica: una revocación es una fila nueva con
 * {@code granted = false}, no un UPDATE de la anterior. Una prueba que se puede editar no prueba
 * nada, y el historial completo es justamente lo que el Decreto 1377 pide poder mostrar.
 * <p>
 * No lleva {@code @DynamicUpdate} como {@link com.jhanp.psraerobicsbackend.modules.person.entity.Person}
 * porque no hay UPDATE del que protegerse, y las columnas de tipo son VARCHAR con CHECK, no enums de
 * Postgres — que es lo que permite escribirla por JPA. Ver la tabla consentimiento en
 * V1__INITIAL_DDL.sql.
 */
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "consentimiento")
public class Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * El id suelto y no un {@code @ManyToOne Person}.
     * <p>
     * La relación no aporta nada aquí: nadie navega de un consentimiento a la ficha de la persona, y
     * sí costaría — al insertar habría que cargar la entidad Person entera solo para poner una clave
     * foránea que ya se tiene a mano. La integridad la garantiza la FK de la tabla.
     */
    @Column(name = "persona_id", nullable = false, updatable = false)
    private Integer personId;

    // Sin @Enumerated: ConsentTypeConverter se aplica solo y guarda el valor en español.
    @Column(name = "tipo", nullable = false, updatable = false, length = 30)
    private ConsentType type;

    /** false = revocación. Artículo 8 de la Ley 1581: revocar es un derecho, y deja rastro propio. */
    @Column(name = "otorgado", nullable = false, updatable = false)
    private Boolean granted;

    /**
     * Qué texto aceptó, no solo que aceptó. Hoy la política es v1 única, pero el día que cambie esta
     * columna es lo único que distingue a quien aceptó lo anterior de quien aceptó lo nuevo.
     */
    @Column(name = "version_politica", nullable = false, updatable = false, length = 10)
    private String policyVersion;

    // Sin @Enumerated: ConsentMediumConverter se aplica solo.
    @Column(name = "medio", nullable = false, updatable = false, length = 10)
    private ConsentMedium medium;

    @Column(name = "fecha", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;

    /**
     * La cuenta que afirmó el consentimiento, cuando había una.
     * <p>
     * <b>null significa "no había sesión"</b>, no "lo hizo la persona". Es el caso del autoregistro,
     * que es público: no hay ningún usuario autenticado a quien atribuirlo, y poner uno inventado
     * sería peor que dejarlo vacío.
     * <p>
     * Con valor puede ser la propia persona —marcando la casilla desde su perfil— o alguien del
     * mostrador en su nombre. Se distinguen comparando este id con la cuenta de la persona; no hace
     * falta una columna para eso.
     */
    @Column(name = "registrado_por", updatable = false)
    private Integer registeredByUserId;

    @PrePersist
    protected void onCreate() {
        this.recordedAt = OffsetDateTime.now();
    }
}
