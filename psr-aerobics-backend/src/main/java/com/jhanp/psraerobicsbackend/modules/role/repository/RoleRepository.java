package com.jhanp.psraerobicsbackend.modules.role.repository;

import com.jhanp.psraerobicsbackend.modules.role.entity.Role;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Catálogo fijo: ADMIN, MIEMBRO y PROFESOR se insertan en la migración inicial.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(RoleName name);
}
