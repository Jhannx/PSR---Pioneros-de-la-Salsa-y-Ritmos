package com.jhanp.psraerobicsbackend.modules.person.dto.request;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Entrada de POST /api/personas y POST /api/personas/autoregistro.
 * <p>
 * Un solo DTO para los dos endpoints porque comparten el 100 % de los campos. Lo único que cambia es
 * fuente_registro, y eso lo decide la ruta, no el cliente: si viniera en el body, cualquiera podría
 * marcar un registro manual como autoregistro.
 * <p>
 * Toda persona registrada nace con cuenta de acceso, así que no hay un endpoint aparte para crearla.
 */
public record PersonRequest(
        @NotNull(message = "El tipo de documento es obligatorio")
        DocumentType documentType,

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 20, message = "El número de documento no puede superar los 20 caracteres")
        String documentNumber,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String firstName,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
        String lastName,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone,

        /**
         * Obligatoria en las altas nuevas, aunque la columna sea nullable: la base tiene que admitir
         * null por las personas registradas antes de que este campo existiera, y este DTO es el único
         * sitio donde se puede distinguir un alta nueva de una fila histórica.
         * <p>
         * @Past y no @PastOrPresent: nadie se inscribe a aeróbicos el día que nace.
         */
        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate birthDate,

        /**
         * Opcional. Se guarda en usuario.correo, no en persona: es un dato de la cuenta, y es lo que
         * permite entrar por correo además de por documento y recibir el aviso de alta. Sin él la
         * cuenta funciona igual, solo que se identifica únicamente por el número de documento.
         */
        @Email(message = "El formato del correo no es válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String email,

        // null → el backend hashea el número de documento como contraseña inicial (RF-01).
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        /*
         * Los tres campos de abajo son la constancia de consentimiento, y NO llevan anotación de
         * validación aunque sean obligatorios.
         *
         * La razón es que cuál de ellos hace falta depende de la ruta, y una anotación no puede
         * saberlo: el autoregistro exige los dos primeros y el alta de mostrador exige el tercero.
         * Un @AssertTrue sobre cualquiera de ellos rompería la otra puerta. La regla vive en
         * RegisterService, que es el único punto por el que pasan las dos altas — el mismo reparto
         * que ya tiene la validación de mayoría de edad.
         */

        /**
         * Numeral 4.1 de la política: autorización general de tratamiento. Obligatorio en el
         * autoregistro, donde la persona marca la casilla ella misma.
         */
        Boolean dataProcessingConsent,

        /**
         * Numeral 12: "entiendo que la actividad física conlleva riesgo de lesión y participo de
         * manera voluntaria". Casilla separada de la anterior, porque es una declaración distinta y
         * la política la declara independiente del tratamiento de datos.
         */
        Boolean voluntaryParticipationConsent,

        /**
         * Alta de mostrador: confirma que se recogió el formato físico firmado, que es como se
         * autoriza cuando el registro lo teclea recepción y no hay casilla en pantalla.
         * <p>
         * Cubre los dos consentimientos anteriores de una vez, porque el papel lleva los dos textos.
         * Se exige —y no se asume por el hecho de venir por esta ruta— por lo mismo que el frontend
         * no da por buena su propia casilla: una fila que afirma que existe un papel firmado sin que
         * nadie lo haya afirmado no prueba nada.
         */
        Boolean signedPhysicalConsent,

        /*
         * Los tres campos de abajo son OPCIONALES y existen para que quien se registra pueda dejar sus
         * datos de emergencia y de salud de una vez, sin tener que volver a entrar después.
         *
         * No es una comodidad: tres de las cuatro pantallas de registro son públicas, así que quien
         * las usa no tiene token todavía y no puede llamar a /contactos-emergencia ni a
         * /observaciones-salud, que exigen sesión. Encadenar las llamadas después del alta tampoco
         * sirve — no hay con qué autenticarlas. El único sitio donde esto se puede resolver es dentro
         * de la transacción del alta, y por eso viajan aquí.
         *
         * Las reglas no se relajan por venir por este camino: las aplican los mismos servicios que
         * atienden los endpoints sueltos. Ver RegisterService.
         */

        /**
         * Dato sensible y opcional (numeral 4.2). Si viene con contenido, exige
         * {@link #healthDataConsent}; si viene vacío o ausente, no exige nada.
         */
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres")
        String healthObservations,

        /**
         * La autorización expresa del numeral 4.2. Solo hace falta cuando hay observaciones que
         * guardar — pedir permiso para no guardar nada sería absurdo, y el propio numeral dice que no
         * suministrar el dato no puede costarle nada a la persona.
         */
        Boolean healthDataConsent,

        /**
         * Hasta cinco, el mismo tope que POST /api/personas/{id}/contactos-emergencia, y cada uno con
         * su propia declaración del numeral 5: son personas distintas, y estar autorizado a entregar
         * los datos de una no dice nada sobre la otra.
         * <p>
         * {@code @Valid} para que las validaciones de cada contacto —teléfono obligatorio y con
         * formato— se apliquen también aquí dentro; sin esta anotación, la lista se validaría como
         * lista y sus elementos no se mirarían.
         */
        @Size(max = 5, message = "No se pueden registrar más de 5 contactos de emergencia")
        List<@Valid EmergencyContactRequest> emergencyContacts
) {
}
