package com.jhanp.psraerobicsbackend.config;

import com.jhanp.psraerobicsbackend.modules.aerobics.history.enums.AuditableTable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Conversión de texto a enum en rutas y parámetros de consulta.
 * <p>
 * Spring convierte texto a enum con {@code Enum.valueOf()}, que compara contra el NOMBRE de la
 * constante Java. En este proyecto las constantes están en inglés y los valores de la base en
 * español, así que para {@code AuditableTable} eso significa que
 * {@code GET /api/auditoria/historial-cambios/registro/PERSONA/5} fallaba con
 * "No enum constant ...AuditableTable.PERSONA", y solo habría funcionado escribiendo {@code PERSON}
 * — un valor que no aparece en ninguna respuesta de la API y que nadie tendría por qué adivinar.
 * <p>
 * Los AttributeConverter de JPA no ayudan aquí: solo actúan entre la entidad y la base, nunca en la
 * capa web.
 * <p>
 * Se aceptan las dos formas —el valor de la base y el nombre de la constante— porque no cuesta nada
 * y evita romper a cualquiera que ya estuviera llamando con la segunda.
 */
@Configuration
public class WebConversionConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addConverter(new StringToAuditableTable());
    }

    static class StringToAuditableTable implements Converter<String, AuditableTable> {

        @Override
        public AuditableTable convert(@NonNull String source) {
            String value = source.trim().toUpperCase();

            try {
                // Primero el valor de la base ('PERSONA'), que es el que el cliente ve en las
                // respuestas y por tanto el que va a mandar.
                return AuditableTable.fromDbValue(value);

            } catch (IllegalArgumentException ignored) {
                // Y si no, el nombre de la constante ('PERSON'). Si tampoco cuadra, la
                // IllegalArgumentException que sale de aquí la traduce GlobalExceptionHandler a un
                // 400 vía MethodArgumentTypeMismatchException.
                return AuditableTable.valueOf(value);
            }
        }
    }
}
