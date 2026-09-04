package com.jhanp.psraerobicsbackend.modules.role.repository;

import com.jhanp.psraerobicsbackend.modules.role.entity.UserRole;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Un usuario puede tener varios roles a la vez: las authorities se cargan desde aquí, no desde
 * un campo en usuario.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    List<UserRole> findByUserId(Integer userId);

    List<UserRole> findByRoleName(RoleName roleName);

    boolean existsByUserIdAndRoleName(Integer userId, RoleName roleName);
}
