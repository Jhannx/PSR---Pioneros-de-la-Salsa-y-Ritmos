package com.jhanp.psraerobicsbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class PsrAerobicsBackendApplication {

    /**
     * La academia esta en Colombia y no se va a mover, asi que la zona no se configura: se fija.
     * <p>
     * <b>Que rompia.</b> Sin esto, Java toma {@code ZoneId.systemDefault()} del servidor. En un
     * contenedor eso es UTC, y entonces el dia cambia a las 7 de la noche hora de Colombia — justo en
     * mitad de la clase. Todo lo que cuenta "hoy" (la sesion del momento, el reporte diario, el
     * unico de sesion por fecha) parte la operacion en dos a mitad de camino.
     * <p>
     * <b>Por que tambien arregla el SQL.</b> El driver de PostgreSQL manda un {@code SET TIME ZONE}
     * con la zona por defecto de la JVM al abrir cada conexion, asi que {@code CURRENT_DATE} y
     * {@code now()} dentro de las funciones y los procedimientos quedan en la misma zona que Java sin
     * tocar nada mas. Por eso va aqui y antes de {@code SpringApplication.run}: si se pusiera en un
     * {@code @PostConstruct}, el pool ya podria haber abierto conexiones con la zona vieja.
     * <p>
     * <b>Y por que no basta con la variable TZ del hoster.</b> Basta si esta puesta. Fijarla en el
     * codigo hace que no dependa de que alguien se acuerde, y que un cambio de hoster no mueva las
     * fechas sin que nadie lo note. La variable {@code TZ=America/Bogota} sigue siendo util para el
     * sistema operativo del contenedor y para la base de datos; esto cubre el proceso Java pase lo
     * que pase.
     */
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
    }

    public static void main(String[] args) {
        SpringApplication.run(PsrAerobicsBackendApplication.class, args);
    }

}
