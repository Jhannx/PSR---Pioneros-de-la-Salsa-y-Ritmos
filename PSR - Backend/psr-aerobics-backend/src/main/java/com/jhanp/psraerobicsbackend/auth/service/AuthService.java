package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.ChangePasswordRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.LoginRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.RegistrationRequest;
import com.jhanp.psraerobicsbackend.auth.dto.response.AuthResponse;
import com.jhanp.psraerobicsbackend.common.dto.MessageResponse;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import com.jhanp.psraerobicsbackend.modules.user.service.interfaces.IUserService;
import com.jhanp.psraerobicsbackend.security.jwt.service.JwtService;
import com.jhanp.psraerobicsbackend.security.userdetails.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final IUserService userService;
    private final RegisterService registerService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Intento de login para: {}", request.email());

        // Si las credenciales fallan, authenticate() lanza y Spring Security responde 401.
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.getToken(userDetails);

        // Se devuelve el usuario junto al token para que el front sepa de una sola llamada quién
        // entró, qué roles tiene y si sigue con la contraseña temporal.
        UserResponse user = userService.findByIdentifier(request.email());

        log.info("Login correcto para el usuario ID {}", user.id());
        return new AuthResponse(token, user);
    }

    @Override
    public UserResponse register(RegistrationRequest request) {
        return registerService.registerPublic(request);
    }

    @Override
    public MessageResponse changePassword(String identifier, ChangePasswordRequest request) {
        userService.changeOwnPassword(identifier, request.currentPassword(), request.newPassword());
        return new MessageResponse("Contraseña actualizada correctamente.");
    }
}
