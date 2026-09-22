package com.jhanp.psraerobicsbackend.config;

import com.jhanp.psraerobicsbackend.security.handler.SecurityJsonResponse;
import com.jhanp.psraerobicsbackend.security.jwt.filter.JwtAuthenticationFilter;
import com.jhanp.psraerobicsbackend.security.userdetails.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Postura de seguridad: TODO está cerrado salvo lo que se abra aquí explícitamente
 * ({@code .anyRequest().authenticated()}). Cualquier controller que se añada nace protegido; no hay
 * que acordarse de nada para que lo esté, solo para abrirlo.
 * <p>
 * Los permisos por rol van con {@code @PreAuthorize} sobre el método del controller. IMPORTANTE: las
 * autoridades son las constantes en inglés de {@link com.jhanp.psraerobicsbackend.modules.role.enums.RoleName}
 * —ADMIN, MEMBER, TEACHER— y sin prefijo {@code ROLE_}, así que va
 * {@code @PreAuthorize("hasAuthority('ADMIN')")} y NO {@code hasRole('ADMIN')}, que por dentro busca
 * ROLE_ADMIN y nunca encontraría nada.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // habilita @PreAuthorize / @PostAuthorize en cualquier bean
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityJsonResponse securityJsonResponse;

    /**
     * Misma propiedad que usa {@link WebSocketConfig}: los orígenes permitidos son los mismos para
     * HTTP y para el handshake STOMP, y tenerlos en dos sitios distintos es la forma segura de que se
     * desincronicen.
     */
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * setAllowedOriginPatterns y no setAllowedOrigins: con credenciales activas, Spring rechaza el
     * comodín en la lista literal, y los patrones permiten dejarlo abierto en local sin partir la
     * configuración en dos perfiles.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Activa el CorsConfigurationSource de arriba. Sin esta línea el bean existe pero la
                // cadena lo ignora, y el preflight (OPTIONS, que llega SIN token) se rechaza con 401
                // antes de tocar el controller.
                .cors(Customizer.withDefaults())

                // CSRF es un riesgo de sesiones con cookies; no aplica a una API stateless con JWT.
                .csrf(AbstractHttpConfigurer::disable)

                // Las rutas públicas se listan una a una y no como /api/auth/**: con el comodín,
                // cualquier endpoint que se añada mañana bajo /api/auth nacería público sin que nadie
                // lo note.
                .authorizeHttpRequests(auth -> auth
                        // Entrar y recuperar la contraseña: por definición los usa quien todavía no
                        // tiene token.
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()

                        // Autoregistro desde el kiosko: la persona se da de alta sola, sin cuenta
                        // previa. El alta desde admin es POST /api/personas y sí exige ADMIN.
                        .requestMatchers(HttpMethod.POST, "/api/personas/autoregistro").permitAll()

                        // Confirmación de cambio de correo: se abre desde el enlace del buzón nuevo,
                        // y quien lo abre puede no tener sesión. La credencial es el token, de un
                        // solo uso y con caducidad. SOLICITAR el cambio es otro endpoint y sí la
                        // exige: solo el dueño o el mostrador pueden pedirlo.
                        .requestMatchers(HttpMethod.POST, "/api/personas/confirmar-cambio-correo").permitAll()

                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Handshake de SockJS. Va abierto a propósito: SockJS no puede mandar la
                        // cabecera Authorization en sus transportes de respaldo, así que aquí no hay
                        // token que validar. Sin esta línea el WebSocket de la cola responde 401.
                        .requestMatchers("/ws/**").permitAll()

                        // Todo lo demás exige token. Los permisos por rol se declaran con
                        // @PreAuthorize en cada controller, junto al método que protegen.
                        .anyRequest().authenticated()
                )

                // Sin esto, un rechazo en el filtro devuelve la página HTML de error de Tomcat: el
                // @RestControllerAdvice solo cubre lo que ocurre dentro de un controller.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityJsonResponse)  // 401: no sé quién eres
                        .accessDeniedHandler(securityJsonResponse)       // 403: sé quién eres, pero no puedes
                )

                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
