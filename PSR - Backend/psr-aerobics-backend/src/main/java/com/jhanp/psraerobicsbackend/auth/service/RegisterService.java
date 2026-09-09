package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.CreateUserRequest;
import com.jhanp.psraerobicsbackend.auth.dto.response.AdminCreationResult;
import com.jhanp.psraerobicsbackend.auth.dto.request.RegistrationRequest;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import com.jhanp.psraerobicsbackend.modules.user.service.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterService {
    private final IUserService userService;
    private final IInvitationService invitationService;

    @Transactional
    public UserResponse registerPublic(RegistrationRequest request) {
        log.info("Auto-registro público para: {}", request.email());
        return userService.createPublic(request.name(), request.email(), request.password());
    }

    @Transactional
    public UserResponse createByAdmin(CreateUserRequest request) {
        log.info("Alta de usuario desde admin para: {}", request.email());

        AdminCreationResult result = userService.createByAdmin(request);

        // Último paso, ya con el usuario completo y con sus roles: un solo correo con la
        // contraseña temporal (con la que ya puede entrar) y el enlace para cambiarla.
        // Si algo fallara antes, la transacción revierte y nadie recibe el correo de una
        // cuenta que no llegó a existir.
        invitationService.invite(result.user().id(), result.temporaryPassword());

        return result.user();
    }
}
