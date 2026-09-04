package com.jhanp.psraerobicsbackend.security.userdetails.service;

import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.security.userdetails.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * El identificador es documento o correo: una cuenta se creó con uno de los dos y quien inicia
     * sesión escribe el que tenga.
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // findByIdentifierWithRoles y no findByDocumentNumberOrEmail: los roles tienen que venir
        // cargados desde la consulta. Quien llama a getAuthorities() es el filtro JWT, y para
        // entonces la sesión de Hibernate está cerrada.
        User user = userRepository.findByIdentifierWithRoles(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + identifier));
        return new CustomUserDetails(user);
    }
}
