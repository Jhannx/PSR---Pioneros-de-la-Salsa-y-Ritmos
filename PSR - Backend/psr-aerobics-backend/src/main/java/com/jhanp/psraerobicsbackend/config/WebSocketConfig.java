package com.jhanp.psraerobicsbackend.config;

import com.jhanp.psraerobicsbackend.security.websocket.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP sobre WebSocket para que la cola del día se actualice sola: sin esto, el panel del admin y
 * las pantallas de la sala tendrían que hacer polling cada pocos segundos contra /api/cola.
 * <p>
 * Broker en memoria y no RabbitMQ ni ActiveMQ: hay un solo proceso y un puñado de clientes
 * simultáneos (el panel, dos TV). Un broker externo solo haría falta al escalar a varias instancias.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    /**
     * Comparte propiedad con el CORS de HTTP: los orígenes permitidos son los mismos, y tenerlos en
     * dos sitios distintos es la forma segura de que se desincronicen. En producción hay que fijarla.
     */
    @Value("${app.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Solo /topic: la cola es difusión pura, no hay mensajería punto a punto.
        registry.enableSimpleBroker("/topic");

        // El servidor nunca recibe mensajes STOMP del cliente —la cola se escribe por REST— pero el
        // prefijo se declara igual para que un @MessageMapping futuro no quede colgando en /topic.
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Aquí está la autenticación real del WebSocket. El handshake de /ws va abierto en SecurityConfig
     * porque SockJS no puede mandar la cabecera Authorization en sus transportes de respaldo; el token
     * se valida en el frame CONNECT de STOMP, que el cliente sí controla.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins);

        // SockJS aparte, como respaldo: algunas redes con proxy tumban el WebSocket nativo, y las
        // pantallas de la sala tienen que reconectar solas sin que nadie las atienda.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }
}
