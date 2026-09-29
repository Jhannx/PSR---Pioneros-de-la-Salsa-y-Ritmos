package com.jhanp.psraerobicsbackend.config;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.Schedule;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.repository.ScheduleRepository;
import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonRepository;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.role.repository.UserRoleProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

/**
 * Cuentas que deben existir siempre: el super admin, las tres terminales PSR y las dos cuentas
 * compartidas de mostrador (Invitado y Menor de edad).
 * <p>
 * Corre antes que los seeders de prueba ({@code @Order(0)}). Estas cuentas son de producción, no de
 * prueba: sin el admin no hay forma de entrar al sistema la primera vez; sin las terminales, los
 * equipos físicos de entrada no pueden operar.
 * <p>
 * <b>Encendido por defecto, pero apagable</b> ({@code app.seed.production.enabled}). El valor por
 * defecto es {@code true} porque es lo que resuelve el arranque en frío, y quien despliega no debería
 * tener que acordarse de encenderlo. La bandera existe para poder apagarlo una vez las cuentas reales
 * estén creadas y no se quiera que un reinicio vuelva a mirar la base, y para poder levantar la app
 * contra una base ajena sin escribir en ella.
 * <p>
 * Idempotente: si la cuenta ya existe (por correo, o por documento en las compartidas), no la toca.
 * Reiniciar la app veinte veces no cambia nada. Una contraseña cambiada desde la aplicación no se
 * pisa. Y al ser idempotente <b>no borra</b>: las terminales 4 y 5 que sembraron las versiones
 * anteriores siguen en la base donde ya corrió, y hay que desactivarlas a mano.
 * <p>
 * <b>Dos clases de cuenta, y la diferencia importa.</b> El admin y las terminales son cuentas
 * técnicas <i>sin persona asociada</i>: el trigger {@code trg_validar_persona_requerida} lo permite
 * para ADMIN y PSR_TERMINAL porque no hay nadie detrás. En auditoría aparecen por su correo en vez
 * de por un nombre, y {@code vista_historial_cambios} ya lo contempla. Invitado y Menor de edad, en
 * cambio, <i>sí</i> llevan persona: tienen que poder entrar a la fila y que se les marque asistencia,
 * y eso es {@code llegada.persona_id}, así que sin persona no existen para el check-in.
 *
 * @see #seedSharedAttendee lo que estas dos cuentas compartidas pueden y no pueden registrar
 */
@Component
// matchIfMissing: sin la propiedad definida, corre igual. Un despliegue al que se le olvide la
// variable arranca con admin y terminales, que es el fallo hacia el lado útil; lo contrario deja el
// sistema sin ninguna cuenta con la que entrar.
@ConditionalOnProperty(name = "app.seed.production.enabled", havingValue = "true", matchIfMissing = true)
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class ProductionAccountsSeeder implements ApplicationRunner {

    /**
     * Credenciales del admin de arranque. Vienen de variables de entorno desde el 28-09-2026: antes
     * eran dos constantes, o sea la contraseña del administrador escrita en el repositorio, legible
     * por cualquiera con acceso al código y sin forma de rotarla sin recompilar.
     * <p>
     * Conservan un valor por defecto a propósito, y ese es el punto delicado: si fueran obligatorias,
     * un despliegue sin las variables arrancaría <i>sin ningún admin</i> y no habría por dónde entrar
     * a crear las cuentas reales. Con defecto, el peor caso es una contraseña conocida que se cambia
     * desde la aplicación en el primer login — recuperable. Sin defecto, el peor caso es un sistema
     * cerrado por fuera.
     * <p>
     * Campos y no constructor porque {@code @RequiredArgsConstructor} de Lombok no copia
     * {@code @Value} a los parámetros que genera, así que en el constructor no se inyectarían.
     * <p>
     * <b>Cambiar el correo no renombra al admin existente.</b> La siembra es idempotente por correo:
     * apuntar {@code ADMIN_EMAIL} a otra dirección sobre una base donde ya corrió crea un segundo
     * admin y deja el primero intacto, con su contraseña vieja.
     * <p>
     * <b>El default del placeholder no basta, y por eso estan los DEFAULT_ de abajo.</b>
     * {@code ${VAR:defecto}} aplica el defecto solo cuando la propiedad <i>no existe</i>. Una variable
     * presente pero vacia —{@code ADMIN_PASSWORD=} en un .env, que es lo natural al dejar un hueco
     * para rellenar despues— resuelve a cadena vacia, no al defecto. El admin se creaba entonces con
     * {@code encode("")} y el login con la contraseña conocida devolvia 401, sin nada en los logs que
     * apuntara a la causa. Lo mismo un valor que quedo como {@code # comentario} porque el parser del
     * .env no soporta comentarios al final de linea. {@link #orDefault} trata ambos casos como
     * ausencia.
     */
    private static final String DEFAULT_ADMIN_EMAIL = "adminpsraerobicos@gmail.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "adminrootpsraerobicos";

    @Value("${app.seed.production.admin-email:}")
    private String adminEmail;

    @Value("${app.seed.production.admin-password:}")
    private String adminPassword;

    /**
     * El valor configurado, o el de fabrica cuando lo configurado no sirve: nulo, en blanco, o un
     * comentario que se cargo el parser del .env.
     */
    private static String orDefault(String configured, String fallback) {
        if (configured == null) {
            return fallback;
        }

        String trimmed = configured.trim();

        return trimmed.isEmpty() || trimmed.startsWith("#") ? fallback : trimmed;
    }

    /** Tres equipos físicos de entrada. Eran cinco; PSR opera con tres. */
    private static final int TERMINAL_COUNT = 3;
    private static final String TERMINAL_EMAIL_PATTERN = "terminal%d@psraerobicos.com";
    private static final String TERMINAL_PASSWORD_PATTERN = "psrterminalaerobicos.%d";

    /**
     * Documentos de las dos cuentas compartidas. No son numéricos a propósito: un documento inventado
     * con pinta de cédula (1000000001 y parecidos) colisiona el día que llegue la persona real que lo
     * tiene, y entonces no hay forma de darla de alta. Estos dos no pueden chocar con nadie, y en
     * cualquier reporte se leen solos.
     */
    private static final String GUEST_DOCUMENT = "INVITADO";
    private static final String MINOR_DOCUMENT = "MENOR";

    /** {@code persona.telefono} es NOT NULL y ninguna de las dos tiene uno. Mismo relleno que el sentinel de V1. */
    private static final String SHARED_PHONE = "0000000000";

    /**
     * No es un hash de bcrypt, así que {@code matches()} devuelve false contra cualquier contraseña:
     * nadie inicia sesión como Invitado ni como Menor de edad. Es el mismo recurso que V1 usa para el
     * usuario sentinel, y hace falta porque {@code sp_registrar_usuario_para_persona} crea la cuenta
     * con rol MIEMBRO — sin esto serían dos cuentas de miembro con contraseña conocida.
     */
    private static final String NO_LOGIN_HASH = "!SHARED_ACCOUNT_NO_LOGIN_POSSIBLE!";

    /**
     * La agenda real de PSR: lunes a jueves, de 7 a 8 de la noche. Viernes no hay clase.
     * <p>
     * Días en ISO, que es lo que exige {@code chk_horario_dia_semana} y lo que lee
     * {@code fn_obtener_o_crear_sesion_del_momento}: 1 = lunes … 7 = domingo.
     */
    private static final int[] CLASS_DAYS = {1, 2, 3, 4};
    private static final LocalTime CLASS_START = LocalTime.of(19, 0);
    private static final LocalTime CLASS_END = LocalTime.of(20, 0);

    private final UserRepository userRepository;
    private final UserRoleProcedureRepository userRoleProcedureRepository;
    private final PersonRepository personRepository;
    private final PersonProcedureRepository personProcedureRepository;
    private final UserProcedureRepository userProcedureRepository;
    private final ScheduleRepository scheduleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedTerminals();

        // CC para el invitado y TI para el menor: la tarjeta de identidad es justo el documento de
        // quien no tiene cédula todavía, así que el tipo dice por sí solo de quién es la fila.
        seedSharedAttendee(GUEST_DOCUMENT, DocumentType.CC, "Invitado", "PSR");
        seedSharedAttendee(MINOR_DOCUMENT, DocumentType.TI, "Menor", "De Edad");

        seedSchedules();
    }

    private void seedAdmin() {
        String email = orDefault(adminEmail, DEFAULT_ADMIN_EMAIL);
        String password = orDefault(adminPassword, DEFAULT_ADMIN_PASSWORD);

        // Que se uso y que no, porque es la diferencia entre "no puedo entrar" y "ya se por que":
        // asi el arranque dice si la variable llego o si se cayo al valor de fabrica, sin imprimir
        // ninguna contraseña.
        if (password.equals(DEFAULT_ADMIN_PASSWORD)) {
            log.warn("ADMIN_PASSWORD no llego (ausente o vacia): se usa la del repositorio. "
                    + "Cambiala en el primer login.");
        }

        if (userRepository.existsByEmail(email)) {
            // Importante cuando el login falla: la cuenta conserva la contraseña con la que nacio.
            // Cambiar ADMIN_PASSWORD ahora no la actualiza; hay que cambiarla desde la aplicacion o
            // borrar la fila y reiniciar.
            log.info("La cuenta de admin ya existe ({}); no se toca, conserva su contraseña actual",
                    email);
            return;
        }

        User admin = userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .active(true)
                .build());
        userRoleProcedureRepository.assign(admin.getId(), RoleName.ADMIN);

        // Solo el correo. La contraseña no se registra ni en WARN: los logs de produccion se
        // guardan, se envian a un agregador y los lee gente que no deberia poder entrar como admin.
        log.warn("Cuenta de admin de producción creada: {}", email);
    }

    private void seedTerminals() {
        for (int i = 1; i <= TERMINAL_COUNT; i++) {
            String email = String.format(TERMINAL_EMAIL_PATTERN, i);
            String password = String.format(TERMINAL_PASSWORD_PATTERN, i);

            if (userRepository.existsByEmail(email)) {
                log.info("La cuenta terminal {} ya existe; no se toca", email);
                continue;
            }

            User terminal = userRepository.saveAndFlush(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .active(true)
                    .build());
            userRoleProcedureRepository.assign(terminal.getId(), RoleName.PSR_TERMINAL);

            log.warn("Cuenta de terminal de producción creada: {}", email);
        }
    }

    /**
     * Una de las dos cuentas compartidas del mostrador: la persona bajo la que se registra quien
     * entra sin cuenta propia.
     * <p>
     * <b>Invitado</b> es para el ingreso rápido — alguien llega, la clase ya empezó y darlo de alta
     * en condiciones (documento, teléfono, fecha de nacimiento, contactos de emergencia) no cabe en
     * ese momento. <b>Menor de edad</b> es lo mismo, pero separado para que conste que quien entró
     * era menor sin guardar el dato de un menor identificado, que es justo lo que la Ley 1581 obliga
     * a tratar con autorización del representante legal. Dos filas anónimas y ningún dato personal
     * de nadie.
     * <p>
     * <b>Lo que estas cuentas SÍ registran y lo que NO.</b> Registran que hubo un invitado (o un
     * menor) en esa sesión. NO registran cuántos: {@code uq_llegada_persona_sesion} es único por
     * (persona, sesión) y exceptúa solo al sentinel 999999999, y además {@code sp_registrar_llegada}
     * devuelve la llegada existente en vez de crear una segunda. El segundo invitado de la misma
     * clase reusa la llegada del primero, y como {@code asistencia.llegada_id} es UNIQUE tampoco
     * genera un segundo cobro. Es una decisión tomada y no un descuido: contar de verdad exige
     * ampliar la excepción de ese índice a estos dos ids en una migración nueva. Mientras tanto, el
     * dinero de los invitados 2..N se cobra fuera del sistema.
     * <p>
     * Se crean por los mismos procedimientos que el alta real, así que quedan exactamente como
     * quedaría cualquier persona: {@code sp_registrar_usuario_para_persona} les pone rol MIEMBRO y
     * las deja activas, que es lo que {@code fn_validar_persona_activa} exige para dejarlas entrar a
     * la fila. Iniciar sesión no pueden — ver {@link #NO_LOGIN_HASH}.
     * <p>
     * Aparecen en los buscadores de persona como cualquier otra, y tiene que ser así: el mostrador
     * necesita encontrarlas para marcarles la llegada. Su saldo es uno solo y compartido.
     *
     * @param document el documento hace de marca de idempotencia: si ya existe, no se toca nada
     */
    private void seedSharedAttendee(String document, DocumentType documentType,
                                    String name, String lastName) {
        if (personRepository.existsByDocumentNumber(document)) {
            log.info("La cuenta compartida {} ya existe; no se toca", document);
            return;
        }

        // Sin fecha de nacimiento (la columna es nullable) porque no hay una: no es una persona, es
        // un cajón. Y sin correo, que es lo que permite chk_usuario_identificador cuando la cuenta
        // se identifica solo por documento.
        Integer personId = personProcedureRepository.registerPerson(
                documentType, document, name, lastName, SHARED_PHONE, null, RegistrationSource.ADMIN);

        userProcedureRepository.registerUserForPerson(personId, NO_LOGIN_HASH, null);

        log.warn("Cuenta compartida de producción creada: {} ({} {})", document, name, lastName);
    }

    /**
     * La agenda semanal. Sin ella no hay clase a la que llegar: {@code sp_registrar_llegada} resuelve
     * la sesión con {@code fn_obtener_o_crear_sesion_del_momento}, que busca el horario activo del día
     * y la hora — con la tabla vacía, todo check-in automático falla y solo entra el MANUAL, que es el
     * único autorizado a operar fuera de horario.
     * <p>
     * Está aquí y no en {@code TestDataSeeder} desde el 28-09-2026. Era el único dato de operación
     * real que vivía en el seed de prueba, así que apagar ese seed para producción — que es lo que hay
     * que hacer con él — dejaba la agenda vacía. Un horario no es un dato de prueba: es la
     * configuración del negocio.
     * <p>
     * <b>Siembra, no sincroniza.</b> Crea el horario que falta y no toca ninguno existente: cambiar
     * la agenda es del admin desde su pantalla, y un seeder que la reescribiera en cada arranque
     * pisaría ese cambio al siguiente reinicio. Por eso mover la clase de las 7 a las 8 aquí no
     * mueve la de una base donde ya se sembró: añadiría una segunda.
     */
    private void seedSchedules() {
        for (int dayOfWeek : CLASS_DAYS) {
            // Identificado por día + hora de inicio, que es lo único que lo hace único en la práctica:
            // la tabla no tiene UNIQUE, así que sin este control cada reinicio apilaría otro cuadro
            // idéntico encima del anterior.
            boolean exists = scheduleRepository
                    .findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(dayOfWeek).stream()
                    .anyMatch(existing -> existing.getStartTime().equals(CLASS_START));

            if (exists) {
                log.info("El horario del día {} a las {} ya existe; no se toca", dayOfWeek, CLASS_START);
                continue;
            }

            scheduleRepository.save(Schedule.builder()
                    .dayOfWeek(dayOfWeek)
                    .startTime(CLASS_START)
                    .endTime(CLASS_END)
                    .active(true)
                    .build());

            log.warn("Horario de producción creado: día {} de {} a {}", dayOfWeek, CLASS_START, CLASS_END);
        }
    }
}
