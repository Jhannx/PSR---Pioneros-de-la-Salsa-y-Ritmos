package com.jhanp.psraerobicsbackend.modules.user.repository;

import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByDocumentNumber(String documentNumber);

    /**
     * Login: la cuenta se identifica por documento o por correo, según con cuál se haya creado.
     */
    Optional<User> findByDocumentNumberOrEmail(String documentNumber, String email);

    /**
     * La misma búsqueda de login, pero con los roles y la persona ya cargados. La usa
     * CustomUserDetailsService, y todo lo que cuelgue del usuario tiene que venir resuelto desde aquí:
     * quien lo lee es el filtro JWT (getAuthorities) y OwnershipGuard (getPerson), y para entonces la
     * sesión de Hibernate ya está cerrada — un LAZY sin resolver reventaría con
     * LazyInitializationException.
     * <p>
     * LEFT JOIN en la persona y no JOIN a secas: una cuenta ADMIN puede no tener persona asociada, y
     * un INNER la dejaría fuera del resultado. Es decir, el administrador no podría entrar.
     * <p>
     * Solo se hace fetch de UNA colección (userRoles); person es ManyToOne. Añadir un segundo fetch de
     * colección aquí lanzaría MultipleBagFetchException al arrancar.
     */
    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.userRoles ur
            JOIN FETCH ur.role
            LEFT JOIN FETCH u.person
            WHERE u.documentNumber = :identifier OR u.email = :identifier
            """)
    Optional<User> findByIdentifierWithRoles(@Param("identifier") String identifier);

    Optional<User> findByPersonId(Integer personId);

    /**
     * Las cuentas de un lote de personas, para resolver PersonResponse.userId al listar sin caer en
     * N+1: una consulta por página en vez de una por persona. La FK vive en usuario, así que Person
     * no puede traer el id de su cuenta por sí sola.
     */
    List<User> findByPersonIdIn(Collection<Integer> personIds);

    boolean existsByEmail(String email);

    /**
     * La misma comprobación, pero como la hace la base: el índice que garantiza la unicidad es
     * uq_usuario_correo_lower, sobre lower(correo). Con existsByEmail a secas, un correo guardado
     * como "Ana@x.com" no aparecería al preguntar por "ana@x.com" y el INSERT reventaría después
     * contra ese índice, con un error de base de datos en vez de un 409 explicable.
     */
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocumentNumber(String documentNumber);

    /**
     * El mismo buscador de un solo campo que {@code PersonRepository.search}, pero sobre cuentas: lo
     * usa la pantalla de roles y accesos, que hasta ahora solo podía paginar a ciegas.
     * <p>
     * Busca por el nombre de la persona detrás de la cuenta, por su documento y por el correo. LEFT
     * JOIN y no INNER: las cuentas técnicas (sistema, terminal) no tienen persona, y con un INNER
     * desaparecerían del listado en cuanto alguien escribiera algo en el buscador.
     * <p>
     * Nativa y con unaccent() por lo mismo que la de personas: "perez" tiene que encontrar a "Pérez".
     * Se compara contra el nombre completo concatenado para que "ana pérez" también funcione.
     * <p>
     * El ORDER BY fijo evita que la paginación repita o se salte filas, que es lo que pasa cuando el
     * motor puede devolverlas en cualquier orden. Como arriba, esto implica que el método NO admite
     * un Pageable con Sort: Spring añadiría un segundo ORDER BY.
     */
    @Query(value = """
            SELECT u.*
            FROM usuario u
            LEFT JOIN persona p ON p.id = u.persona_id
            WHERE unaccent(LOWER(COALESCE(p.nombre || ' ' || p.apellidos, ''))) LIKE unaccent(LOWER('%' || :term || '%'))
               OR COALESCE(u.numero_documento, '') LIKE '%' || :term || '%'
               OR unaccent(LOWER(COALESCE(u.correo, ''))) LIKE unaccent(LOWER('%' || :term || '%'))
            ORDER BY p.nombre, p.apellidos, u.id
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM usuario u
            LEFT JOIN persona p ON p.id = u.persona_id
            WHERE unaccent(LOWER(COALESCE(p.nombre || ' ' || p.apellidos, ''))) LIKE unaccent(LOWER('%' || :term || '%'))
               OR COALESCE(u.numero_documento, '') LIKE '%' || :term || '%'
               OR unaccent(LOWER(COALESCE(u.correo, ''))) LIKE unaccent(LOWER('%' || :term || '%'))
            """,
            nativeQuery = true)
    Page<User> search(@Param("term") String term, Pageable pageable);

    /**
     * Usuarios activos con un rol dado; sirve para listar los profesores asignables a una sesión.
     * <p>
     * Nativa a propósito, como las de búsqueda de arriba. rol.nombre es un enum de Postgres
     * (rol_nombre_enum), pero RoleNameConverter lo bindea como varchar, y Postgres no sabe comparar
     * los dos tipos: en JPQL esto fallaba con "operator does not exist: rol_nombre_enum = character
     * varying". JPQL no tiene forma de expresar el CAST a un tipo propio de la base, así que la
     * consulta baja a SQL para poder escribirlo.
     * <p>
     * Por eso el parámetro es String y no RoleName: lo que viaja es el valor en español de la base
     * —{@code RoleName.TEACHER.getDbValue()}, PROFESOR—, no el nombre de la constante Java.
     */
    @Query(value = """
            SELECT u.*
            FROM usuario u
            JOIN usuario_rol ur ON ur.usuario_id = u.id
            JOIN rol r ON r.id = ur.rol_id
            WHERE r.nombre = CAST(:roleName AS rol_nombre_enum)
              AND u.activo = true
            """, nativeQuery = true)
    List<User> findActiveByRole(@Param("roleName") String roleName);
}
