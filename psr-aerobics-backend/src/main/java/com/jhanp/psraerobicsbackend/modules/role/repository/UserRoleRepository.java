package com.jhanp.psraerobicsbackend.modules.role.repository;

import com.jhanp.psraerobicsbackend.modules.role.entity.UserRole;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Un usuario puede tener varios roles a la vez: las authorities se cargan desde aquí, no desde
 * un campo en usuario.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    List<UserRole> findByUserId(Integer userId);

    /**
     * Los roles de varias cuentas de una vez, para poder responder un listado de usuarios sin pedir
     * los roles fila por fila.
     * <p>
     * El JOIN FETCH no es opcional: sin él, leer {@code ur.getRole().getName()} de cada resultado
     * dispara una consulta por rol y se vuelve al mismo N+1 que este método existe para evitar.
     */
    @Query("""
            SELECT ur FROM UserRole ur
            JOIN FETCH ur.role
            WHERE ur.user.id IN :userIds
            """)
    List<UserRole> findByUserIdInWithRole(@Param("userIds") Collection<Integer> userIds);

    List<UserRole> findByRoleName(RoleName roleName);

    boolean existsByUserIdAndRoleName(Integer userId, RoleName roleName);
}
