package com.jhanp.psraerobicsbackend.modules.person.entity;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * <b>@DynamicUpdate no es una optimización, es lo que hace que se pueda escribir esta tabla.</b>
 * Por defecto Hibernate incluye TODAS las columnas en cada UPDATE, también las que no cambiaron. Aquí
 * eso arrastraba a fuente_registro —un enum de Postgres que se bindea como varchar a través de
 * RegistrationSourceConverter— y Postgres rechazaba la sentencia entera con "column fuente_registro
 * is of type fuente_registro_enum but expression is of type character varying". Efecto real:
 * desactivar una persona (que solo toca 'activo') respondía 500. Con esto, el UPDATE lleva solo los
 * campos modificados y las columnas enum que nadie tocó no entran en la sentencia.
 */
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@DynamicUpdate
@Table(name = "persona")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * tipo_documento SÍ es editable (PUT /api/personas/{id}), así que no basta con @DynamicUpdate:
     * cuando cambia, la columna entra en el UPDATE y hay que bindearla como el tipo que es.
     * <p>
     * @JdbcTypeCode(NAMED_ENUM) hace que Hibernate envíe el valor con setObject(..., Types.OTHER) y
     * deje que Postgres lo resuelva contra tipo_documento_enum — el mismo mecanismo que ya usa
     * PersonProcedureRepository para pasarle enums a los procedimientos almacenados. Sin esto viaja
     * como varchar y no existe operador entre los dos tipos.
     * <p>
     * Las constantes de DocumentType (CC, TI, CE, PA) coinciden una a una con las etiquetas del enum
     * de la base, por eso aquí basta @Enumerated(STRING) y no hace falta un converter.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_documento", columnDefinition = "tipo_documento_enum")
    private DocumentType documentType;

    @Column(name = "numero_documento", unique = true, length = 20)
    private String documentNumber;

    @Column(name = "nombre", length = 100)
    private String name;

    @Column(name = "apellidos", length = 100)
    private String lastName;

    @Column(name = "telefono", length = 20)
    private String phone;

    /**
     * Puede venir null y hay que contar con ello: la columna es nullable porque las personas
     * registradas antes de que existiera este campo no lo tienen. Que sea obligatorio en las altas
     * nuevas lo impone {@code PersonRequest}, no la base — es el único sitio donde se puede
     * distinguir un alta nueva de una fila histórica.
     */
    @Column(name = "fecha_nacimiento")
    private LocalDate birthDate;

    /**
     * Alergias, condiciones, lo que haya que saber si alguien se desmaya en clase. Vivía en
     * contacto_emergencia cuando el contacto era uno solo; con varios contactos no tendría dueño
     * claro —¿cuál de los cinco la lleva?— así que es de la persona.
     */
    @Column(name = "observaciones_salud")
    private String healthObservations;

    // Sin @Enumerated: RegistrationSourceConverter se aplica solo y guarda el valor en español.
    @Column(name = "fuente_registro", nullable = false, columnDefinition = "fuente_registro_enum")
    private RegistrationSource registrationSource;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "activo", nullable = false)
    private Boolean active;

    /**
     * Hasta cinco, sin orden de prioridad. El tope lo garantiza trg_limitar_contactos_emergencia y lo
     * comprueba antes EmergencyContactService, para poder responder 409 con un mensaje legible.
     * <p>
     * <b>Era un @OneToOne y cambiarlo arregla de paso el N+1 sobre contacto_emergencia.</b> Un
     * @OneToOne del lado inverso (mappedBy) no se puede diferir por mucho que diga LAZY: Hibernate no
     * sabe si hay fila o no sin preguntar, así que no puede fabricar un proxy y lanzaba un SELECT por
     * cada persona cargada — una consulta por fila al listar personas, para un dato que
     * PersonResponse ni siquiera expone. Una colección sí se difiere de verdad.
     */
    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmergencyContact> emergencyContacts;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = OffsetDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
    }
}
