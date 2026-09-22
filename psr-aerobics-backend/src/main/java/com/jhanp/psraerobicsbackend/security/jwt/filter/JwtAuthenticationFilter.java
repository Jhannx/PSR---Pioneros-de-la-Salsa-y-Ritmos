package com.jhanp.psraerobicsbackend.security.jwt.filter;


import com.jhanp.psraerobicsbackend.security.jwt.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. Busca el header Authorization
        final String authHeader = request.getHeader("Authorization");

        // 2. Si no hay header o no empieza con "Bearer ", deja pasar sin autenticar
        //    (puede ser una ruta pública, o simplemente no tiene token)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extrae el token puro, quitando el prefijo "Bearer "
        final String token = authHeader.substring(7);

        // Un token caducado, alterado o simplemente basura es una petición NO AUTENTICADA,
        // no un fallo del servidor. Sin este try la excepción sube hasta Tomcat y el cliente
        // recibe un 500; capturándola seguimos la cadena sin autenticar y acaba en el 401
        // que emite SecurityJsonResponse. Nota de seguridad: no se le cuenta al cliente
        // POR QUÉ falló el token, eso solo ayuda a quien esté probando a forzarlo.
        try {
            // 4. Extrae el username (subject) guardado dentro del token
            final String username = jwtService.getUsernameFromToken(token);

            // 5. Solo procede si hay username Y todavía no hay nadie autenticado en este contexto
            //    (evita re-procesar si ya se autenticó antes en la misma request)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 6. Busca el usuario completo en la BD (vía tu UserDetailsService de siempre)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 7. Verifica que el token sea válido para ESE usuario específico
                if (jwtService.isTokenValid(token, userDetails)) {

                    // 8. Construye el objeto Authentication "a mano", ya autenticado
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // no hay credenciales que guardar, ya se validó con el token
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 9. Lo registra en el contexto de seguridad de Spring para ESTA request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
            // UsernameNotFoundException incluida a propósito: el token puede estar firmado
            // y vigente pero apuntar a un usuario ya borrado de la BD.
            log.debug("Token rechazado en {}: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 10. Continúa la cadena de filtros (deja que la request siga su camino normal)
        filterChain.doFilter(request, response);
    }
}
