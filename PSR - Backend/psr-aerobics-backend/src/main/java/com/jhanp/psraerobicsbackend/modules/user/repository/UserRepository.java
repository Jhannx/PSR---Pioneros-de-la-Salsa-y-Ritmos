package com.jhanp.psraerobicsbackend.modules.user.repository;

import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
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
     * La misma búsqueda de login, pero con los roles ya cargados. La usa CustomUserDetailsService:
     * getAuthorities() se consulta desde el filtro JWT, cuando la sesión de Hibernate ya está
     * cerrada y un LAZY sin resolver reventaría.
     */
    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.userRoles ur
            JOIN FETCH ur.role
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

    boolean existsByDocumentNumber(String documentNumber);

    /**
     * Usuarios activos con un rol dado; sirve para listar los profesores asignables a una sesión.
     */
    @Query("""
            SELECT u FROM User u
            JOIN u.userRoles ur
            WHERE ur.role.name = :roleName
              AND u.active = true
            """)
    List<User> findActiveByRole(@Param("roleName") RoleName roleName);
}
