package com.jhanp.psraerobicsbackend.modules.role.service.impl;

import com.jhanp.psraerobicsbackend.modules.role.dto.response.RoleResponse;
import com.jhanp.psraerobicsbackend.modules.role.entity.Role;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.role.mapper.RoleMapper;
import com.jhanp.psraerobicsbackend.modules.role.repository.RoleRepository;
import com.jhanp.psraerobicsbackend.modules.role.service.interfaces.IRoleService;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoleService implements IRoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    public List<RoleResponse> findAll() {
        log.info("Consultando todos los roles del sistema");
        return roleRepository.findAll().stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    public RoleResponse findById(Integer id) {
        log.info("Buscando rol con ID: {}", id);
        Role role = roleRepository.findById(id).orElseThrow(() -> {
            log.warn("Rol no encontrado con ID: {}", id);
            return new ResourceNotFoundException("Rol no encontrado con ID: " + id);
        });
        return roleMapper.toResponse(role);
    }

    @Override
    public RoleResponse findByName(RoleName name) {
        log.info("Buscando rol por nombre: {}", name);
        Role role = roleRepository.findByName(name).orElseThrow(() -> {
            log.warn("Rol no encontrado con nombre: {}", name);
            return new ResourceNotFoundException("Rol no encontrado con nombre: " + name);
        });
        return roleMapper.toResponse(role);
    }
}
