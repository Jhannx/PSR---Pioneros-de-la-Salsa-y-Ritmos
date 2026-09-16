package com.jhanp.psraerobicsbackend.security.websocket;

import com.jhanp.psraerobicsbackend.security.jwt.service.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Autentica la conexión STOMP en su frame CONNECT.
 * <p>
 * Es la única puerta que tiene el WebSocket. El handshake HTTP de {@code /ws} va en permitAll dentro
 * de {@link com.jhanp.psraerobicsbackend.config.SecurityConfig} y no puede ser de otra forma: SockJS
 * no manda la cabecera {@code Authorization} en sus transportes de respaldo, así que ahí no hay token
 * que validar. Sin este interceptor, cualquiera que alcanzara el puerto podría suscribirse a
 * {@code /topic/cola/admin} y ver montos, saldos y notas.
 * <p>
 * El token viaja como cabecera nativa del frame CONNECT, que el cliente sí controla:
 * <pre>
 *   stompClient.connect({ Authorization: 'Bearer ' + token }, ...)
 * </pre>
 * Solo se valida en CONNECT. Los frames siguientes de esa sesión ya van asociados al usuario que
 * quedó fijado aquí, así que no hace falta repetir la comprobación en cada SUBSCRIBE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Se requiere un token para conectarse");
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtService.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new MessageDeliveryException("Token inválido");
            }

            // A diferencia del filtro HTTP, aquí NO se toca el SecurityContextHolder: es un
            // ThreadLocal y estos mensajes viajan por hilos del broker. El usuario se guarda en la
            // sesión STOMP, que es lo que dura toda la conexión.
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));

            log.debug("Conexión STOMP autenticada para {}", username);

        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
            // No se le dice al cliente POR QUÉ falló: eso solo ayuda a quien esté probando a forzarlo.
            log.debug("Conexión STOMP rechazada: {}", e.getMessage());
            throw new MessageDeliveryException("Token inválido");
        }

        return message;
    }
}
