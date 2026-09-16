package com.jhanp.psraerobicsbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI y, sobre todo, el esquema de autenticación que usa Swagger UI.
 * <p>
 * Sin este bean springdoc genera la documentación igual, pero la UI no muestra el botón
 * <b>Authorize</b>: no hay dónde pegar el JWT, toda prueba sale sin cabecera {@code Authorization} y
 * cualquier endpoint protegido responde 401. Parece que "pide login" cuando en realidad lo que falta
 * es la casilla del token.
 * <p>
 * El {@code addSecurityItem} aplica el esquema a TODAS las operaciones. Es a propósito: las públicas
 * (login, recuperación de contraseña, autoregistro) ignoran la cabecera sobrante, y la alternativa
 * —anotar operación por operación— se olvida en cuanto alguien añade un controller.
 */
@Configuration
public class OpenApiConfig {

    /** Nombre interno del esquema. Solo lo usa la propia definición, no viaja en las peticiones. */
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI psrOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PSR Aeróbicos API")
                        .version("v1")
                        .description("""
                                Para probar endpoints protegidos: POST /api/auth/login con
                                identifier + password, copia el token de la respuesta y pégalo en
                                Authorize. Swagger añade el "Bearer " por su cuenta, no lo escribas."""))

                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))

                // bearerFormat es solo informativo para quien lee la documentación; lo que hace que la
                // UI mande la cabecera es el par type=HTTP + scheme=bearer.
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
