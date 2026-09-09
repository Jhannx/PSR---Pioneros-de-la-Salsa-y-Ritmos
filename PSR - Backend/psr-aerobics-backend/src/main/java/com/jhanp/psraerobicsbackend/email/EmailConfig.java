package com.jhanp.psraerobicsbackend.email;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Se queda en el paquete email y no en config/ para que el módulo sea autocontenido: quien lo lea
 * ve el puerto, las dos implementaciones y su configuración sin salir de aquí.
 */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {
}
