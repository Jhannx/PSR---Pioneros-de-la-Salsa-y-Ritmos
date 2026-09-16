package com.jhanp.psraerobicsbackend.security.userdetails.model;


import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails {
    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // getName() devuelve el enum RoleName; SimpleGrantedAuthority espera el String.
        return user.getUserRoles().stream()
                .map(ur -> new SimpleGrantedAuthority(ur.getRole().getName().name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    /**
     * El correo si lo hay, y si no el número de documento. La cuenta exige uno de los dos
     * (chk_usuario_identificador), pero la mayoría nace solo con documento, y devolver el correo a
     * secas dejaba el subject del JWT en null.
     * <p>
     * El orden importa y tiene que ser estable: este valor es el subject del token, y
     * JwtService.isTokenValid lo compara contra lo que devuelva este mismo método al recargar el
     * usuario. Como no depende de con cuál de los dos se haya iniciado sesión, siempre cuadra.
     */
    @Override
    public String getUsername() {
        return user.getEmail() != null ? user.getEmail() : user.getDocumentNumber();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /**
     * Desactivar a una persona apaga su cuenta por trg_sincronizar_usuario_con_persona. Si esto
     * devolviera true fijo, esa baja no serviría de nada: seguiría pudiendo entrar.
     */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getActive());
    }
}

