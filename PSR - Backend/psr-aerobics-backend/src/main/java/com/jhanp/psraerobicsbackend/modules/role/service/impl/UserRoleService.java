package com.jhanp.psraerobicsbackend.modules.role.service.impl;

import com.jhanp.psraerobicsbackend.modules.user.dto.request.UserRoleRequest;
import com.jhanp.psraerobicsbackend.modules.role.dto.response.RoleResponse;
import com.jhanp.psraerobicsbackend.modules.role.entity.UserRole;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.role.mapper.RoleMapper;
import com.jhanp.psraerobicsbackend.modules.role.repository.UserRoleProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.role.repository.UserRoleRepository;
import com.jhanp.psraerobicsbackend.modules.role.service.interfaces.IUserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserRoleService implements IUserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRoleProcedureRepository userRoleProcedureRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public List<RoleResponse> assign(Integer userId, UserRoleRequest request) {
        userRoleProcedureRepository.assign(userId, request.roleName());
        
        log.info("Rol {} asignado al usuario ID {}", request.roleName(), userId);
        
        return findByUser(userId);
    }

    @Override
    @Transactional
    public void revoke(Integer userId, RoleName roleName) {
        userRoleProcedureRepository.revoke(userId, roleName);
        log.info("Rol {} revocado al usuario ID {}", roleName, userId);
    }

    @Override
    public List<RoleResponse> findByUser(Integer userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRole)
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    public boolean hasRole(Integer userId, RoleName roleName) {
        return userRoleRepository.existsByUserIdAndRoleName(userId, roleName);
    }
}
