package com.jhanp.psraerobicsbackend.modules.person.repository;

import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Integer> {

    Optional<Person> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);

    /**
     * Para avisar de teléfonos repetidos al dar de alta. No hay UNIQUE detrás en la base: la tabla ya
     * tiene datos y un índice único fallaría al aplicarse si hay repetidos previos. Esto no impide
     * que dos personas acaben compartiendo teléfono si alguien lo fuerza por otra vía, pero cubre el
     * único camino por el que hoy entran personas al sistema.
     */
    boolean existsByPhone(String phone);

    /** La misma comprobación al editar, excluyendo a la propia persona que se está editando. */
    boolean existsByPhoneAndIdNot(String phone, Integer id);

    /**
     * Buscador de un solo campo, estilo Google: el mismo texto compara contra nombre, apellidos y
     * número de documento. El frontend no ofrece —ni debe ofrecer— un selector de "buscar por qué".
     * <p>
     * Nativa y no JPQL por {@code unaccent()}, que es una función de Postgres sin equivalente en JPQL:
     * sin ella "perez" no encuentra a "Pérez" ni "munoz" a "Muñoz", que es exactamente lo que la gente
     * teclea. Se aplica a los dos lados para que dé igual dónde esté la tilde, en el dato o en la
     * búsqueda.
     * <p>
     * Se compara contra el nombre COMPLETO concatenado y no contra cada columna por separado: así
     * "ana pérez" encuentra a Ana Pérez, que buscando columna a columna no aparecía nunca —ninguna de
     * las dos contiene la cadena entera—. Buscar solo "ana" o solo "pérez" sigue funcionando, porque
     * el nombre completo también los contiene.
     * <p>
     * El documento va aparte y sin unaccent: son dígitos, no tienen tildes, y así una búsqueda
     * numérica no paga el coste de normalizar.
     * <p>
     * Sin índice detrás: un LIKE con comodín inicial no puede usar uno normal, y montar un índice
     * trigram para unos cientos de personas sería resolver un problema que todavía no existe.
     * <p>
     * El ORDER BY no es cosmético: sin un orden fijo, Postgres puede devolver las filas en distinto
     * orden en cada consulta, y con paginación eso hace que un mismo registro salga en dos páginas
     * mientras otro no sale en ninguna. Se desempata por id porque puede haber homónimos.
     * <p>
     * OJO: por llevar ORDER BY propio, este método NO admite un Pageable con Sort — Spring añadiría
     * un segundo ORDER BY y el SQL no compilaría. El listado se pide sin ?sort=.
     */
    @Query(value = """
            SELECT p.*
            FROM persona p
            WHERE unaccent(LOWER(p.nombre || ' ' || p.apellidos)) LIKE unaccent(LOWER('%' || :term || '%'))
               OR p.numero_documento LIKE '%' || :term || '%'
            ORDER BY p.nombre, p.apellidos, p.id
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM persona p
            WHERE unaccent(LOWER(p.nombre || ' ' || p.apellidos)) LIKE unaccent(LOWER('%' || :term || '%'))
               OR p.numero_documento LIKE '%' || :term || '%'
            """,
            nativeQuery = true)
    Page<Person> search(@Param("term") String term, Pageable pageable);
}
