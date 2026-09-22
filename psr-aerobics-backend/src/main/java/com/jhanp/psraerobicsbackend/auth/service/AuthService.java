package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.ChangePasswordRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.LoginRequest;
import com.jhanp.psraerobicsbackend.auth.dto.response.AuthResponse;
import com.jhanp.psraerobicsbackend.auth.dto.response.AuthenticatedUserResponse;
import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.service.interfaces.IUserService;
import com.jhanp.psraerobicsbackend.security.jwt.service.JwtService;
import com.jhanp.psraerobicsbackend.security.userdetails.model.CustomUserDetails;
import com.jhanp.psraerobicsbackend.shared.mapper.PersonNames;
import com.jhanp.psraerobicsbackend.shared.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {

    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final IUserService userService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Intento de login para: {}", request.identifier());

        // Si las credenciales fallan, authenticate() lanza AuthenticationException y el 401 lo arma
        // GlobalExceptionHandler.handleAuthentication — no el entry point de Spring Security, que
        // solo cubre la cadena de filtros y aquí ya estamos dentro del controller. No comprobamos la
        // contraseña a mano: el AuthenticationManager ya sabe hacerlo, y de paso rechaza a las
        // cuentas desactivadas vía CustomUserDetails.isEnabled().
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identifier(), request.password()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.getToken(userDetails);

        // Se arma desde el principal y no con otra consulta: findByIdentifierWithRoles ya trajo la
        // cuenta con sus roles y su persona, así que el login resuelve con una sola ida a la base.
        AuthenticatedUserResponse user = toAuthenticatedUser(userDetails.getUser());

        log.info("Login correcto para el usuario ID {}", user.userId());
        return new AuthResponse(token, user);
    }

    @Override
    public MessageResponse changePassword(String identifier, ChangePasswordRequest request) {
        userService.changeOwnPassword(identifier, request.currentPassword(), request.newPassword());
        return new MessageResponse("Contraseña actualizada correctamente.");
    }

    /**
     * Todo lo que se lee aquí viene ya resuelto por el JOIN FETCH de findByIdentifierWithRoles. Si
     * algún día se llama a este método con un User traído de otra consulta, hay que comprobar que
     * userRoles y person no queden perezosos.
     */
    private AuthenticatedUserResponse toAuthenticatedUser(User user) {
        List<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName().name())
                .sorted()
                .toList();

        Person person = user.getPerson();

        return new AuthenticatedUserResponse(
                user.getId(),
                person != null ? person.getId() : null,
                user.getDocumentNumber(),
                user.getEmail(),
                PersonNames.fullName(person),
                roles);
    }
}
