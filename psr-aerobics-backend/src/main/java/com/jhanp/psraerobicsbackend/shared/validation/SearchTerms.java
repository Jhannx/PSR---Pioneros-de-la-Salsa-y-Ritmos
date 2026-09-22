package com.jhanp.psraerobicsbackend.shared.validation;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Normalización del texto que llega por los parámetros {@code ?buscar=} de los listados.
 * <p>
 * Vive en shared porque hay más de un buscador —personas y cuentas— y la regla tiene que ser la
 * misma en todos: que el mismo texto encuentre lo mismo mires donde mires es parte del contrato, no
 * un detalle de cada módulo.
 * <p>
 * Las tildes NO se tocan aquí: de eso se encarga {@code unaccent()} en la base, que las quita a los
 * dos lados de la comparación. Hacerlo en Java solo arreglaría el término escrito, no lo guardado.
 */
public final class SearchTerms {

    private SearchTerms() {
    }

    /**
     * Deja el término listo para un LIKE: sin espacios sobrantes en los extremos y con los interiores
     * colapsados a uno solo.
     * <p>
     * Lo segundo importa más de lo que parece: al escribir "ana  pérez" con doble espacio —o al pegar
     * un nombre copiado de otro sitio— la comparación contra "Ana Pérez" fallaba por un carácter que
     * nadie ve. Colapsarlos convierte un fallo invisible en una coincidencia.
     *
     * @return el término limpio, o null si no queda nada útil que buscar
     */
    public static String normalize(String term) {
        if (term == null) {
            return null;
        }

        String normalized = term.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * El mismo Pageable pero sin ordenación. Obligatorio antes de llamar a cualquiera de las
     * consultas de búsqueda.
     * <p>
     * <b>Por qué existe:</b> las búsquedas de personas y de cuentas son SQL nativo y llevan su propio
     * {@code ORDER BY} —tienen que llevarlo, porque sin un orden fijo la paginación repite y se salta
     * filas—. Si además llega un Pageable con Sort, Spring Data le concatena un segundo
     * {@code ORDER BY} y el SQL deja de ser válido: la consulta revienta y el listado responde 500.
     * <p>
     * Estaba documentado en el javadoc de los repositorios como "este método NO admite Sort", pero no
     * había nada que lo impidiera: bastaba con que alguien añadiera {@code ?sort=firstName} a la URL
     * para tumbar el endpoint. Un comentario no es una defensa.
     */
    public static Pageable unsorted(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}
