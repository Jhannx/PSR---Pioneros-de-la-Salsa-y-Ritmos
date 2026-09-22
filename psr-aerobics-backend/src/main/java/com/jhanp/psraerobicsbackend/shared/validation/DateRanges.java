package com.jhanp.psraerobicsbackend.shared.validation;

import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import lombok.extern.slf4j.Slf4j;

import java.time.temporal.Temporal;

/**
 * Validación de los rangos desde/hasta que reciben los listados por fecha.
 * <p>
 * Existe porque el mismo bloque de tres líneas estaba copiado en ChangeHistoryService y en
 * ReportService, y faltaba en los otros tres sitios que reciben un rango. Esa es justo la forma en
 * que un endpoint acaba devolviendo 500: nadie valida, el null llega a la consulta y revienta abajo.
 * <p>
 * Se valida en el service y no solo con {@code @NotNull} en el controller a propósito: los rangos
 * también se arman por dentro —el dashboard calcula su ventana— y ahí no hay anotación que valga.
 */
@Slf4j
public final class DateRanges {

    private DateRanges() {
    }

    /**
     * Exige un rango completo y coherente. Sirve para cualquier tipo de fecha comparable, así que vale
     * igual para los rangos por día (LocalDate) que para los del historial, que llevan hora
     * (OffsetDateTime).
     * <p>
     * Los tres fallos van juntos y con el mismo mensaje porque para quien llama son el mismo error:
     * el rango no sirve. Distinguir "falta hasta" de "desde es posterior" no le cambia nada.
     *
     * @throws InvalidDataException si falta alguno de los dos extremos, o si están al revés
     */
    public static <T extends Temporal & Comparable<? super T>> void require(T from, T to) {
        if (from == null || to == null) {
            log.warn("Rango de fechas incompleto: desde={}, hasta={}", from, to);
            throw new InvalidDataException("Los parámetros desde y hasta son obligatorios y deben enviarse juntos");
        }

        if (from.compareTo(to) > 0) {
            log.warn("Rango de fechas inválido: desde ({}) es posterior a hasta ({})", from, to);
            throw new InvalidDataException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }
}
