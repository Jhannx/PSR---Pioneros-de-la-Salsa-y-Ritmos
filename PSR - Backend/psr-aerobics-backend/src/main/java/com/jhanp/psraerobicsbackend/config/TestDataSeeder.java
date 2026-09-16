package com.jhanp.psraerobicsbackend.config;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.Schedule;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.repository.ScheduleRepository;
import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonRepository;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.role.repository.UserRoleProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Siembra datos de prueba al arrancar: cinco cuentas por rol, los casos raros que conviene tener
 * siempre a mano, la cuenta técnica de la terminal y un cuadro de horarios.
 * <p>
 * Resuelve el problema del huevo y la gallina: toda cuenta nace con rol MIEMBRO
 * (sp_registrar_usuario_para_persona) y ascender a alguien exige ser ADMIN, así que en una base recién
 * migrada no hay forma de crear el primer administrador desde la API.
 * <p>
 * ¿Por qué en Java y no en el .sql como los roles? Porque la contraseña tiene que pasar por BCrypt, y
 * BCrypt produce un hash distinto cada vez (lleva sal aleatoria): no existe una cadena fija que se
 * pueda escribir en un INSERT. Sembrar desde Java, con el mismo PasswordEncoder que usa el login, es
 * la única forma de que las credenciales funcionen de verdad.
 * <p>
 * Solo corre con {@code app.seed.enabled=true}. Déjalo apagado fuera de tu máquina: crea cuentas con
 * contraseñas conocidas, que es exactamente lo que no quieres en un entorno real.
 * <p>
 * Es idempotente: si la persona o el horario ya existen, no los toca. Se puede reiniciar sin miedo.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class TestDataSeeder implements ApplicationRunner {

    private final PersonRepository personRepository;
    private final PersonProcedureRepository personProcedureRepository;
    private final UserProcedureRepository userProcedureRepository;
    private final UserRepository userRepository;
    private final UserRoleProcedureRepository userRoleProcedureRepository;
    private final ScheduleRepository scheduleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Una persona de prueba. {@code withEmail} en false deja la cuenta identificada solo por
     * documento, que es el caso del kiosko.
     */
    private record SeedPerson(String documentNumber, String firstName, String lastName,
                              boolean withEmail, RoleName... roles) {

        SeedPerson(String documentNumber, String firstName, String lastName, RoleName... roles) {
            this(documentNumber, firstName, lastName, true, roles);
        }
    }

    /**
     * Cinco cuentas por rol.
     * <p>
     * Los documentos son cortos y agrupados por rol —11xx admin, 22xx profesor, 33xx recepción, 44xx
     * miembro— porque son también la contraseña, y en pruebas se teclean cien veces al día. Un
     * documento realista de diez dígitos aquí solo sirve para equivocarse al escribirlo.
     * <p>
     * Los apellidos llevan tildes y eñes a propósito —Martínez, Muñoz, Peña—, y no es decoración: son
     * los datos con los que se comprueba que el buscador encuentra "martinez" escrito sin tilde, que
     * es como lo teclea todo el mundo. Con nombres planos ese fallo no aparecería nunca en pruebas.
     */
    private static final List<SeedPerson> PEOPLE = List.of(

            // ADMIN — con estas entras al panel y puedes dar de alta desde POST /api/personas.
            new SeedPerson("1111", "Miguel", "Martínez", RoleName.ADMIN),
            new SeedPerson("1112", "Laura", "Gómez", RoleName.ADMIN),
            new SeedPerson("1113", "Andrés", "Peña", RoleName.ADMIN),
            new SeedPerson("1114", "Carolina", "Ríos", RoleName.ADMIN),
            new SeedPerson("1115", "Tomás", "Cifuentes", RoleName.ADMIN),

            // PROFESOR — los que salen en el selector de sesiones (GET /api/usuarios/profesores).
            new SeedPerson("2222", "Carlos", "Muñoz", RoleName.TEACHER),
            new SeedPerson("2223", "Diana", "Quintero", RoleName.TEACHER),
            new SeedPerson("2224", "Julián", "Ospina", RoleName.TEACHER),
            new SeedPerson("2225", "Paola", "Cárdenas", RoleName.TEACHER),
            new SeedPerson("2226", "Ricardo", "Salazar", RoleName.TEACHER),

            // RECEPCION — el mostrador: cola, check-in, pagos y abonos.
            new SeedPerson("3333", "Sandra", "Valencia", RoleName.RECEPTIONIST),
            new SeedPerson("3334", "Jorge", "Betancur", RoleName.RECEPTIONIST),
            new SeedPerson("3335", "Mónica", "Zapata", RoleName.RECEPTIONIST),
            new SeedPerson("3336", "Felipe", "Arango", RoleName.RECEPTIONIST),
            new SeedPerson("3337", "Lucía", "Naranjo", RoleName.RECEPTIONIST),

            // MIEMBRO — los que llenan la cola y las listas. Ana Pérez es la del plan de pruebas:
            // "buscar=an" tiene que encontrarla, y "perez" sin tilde también.
            new SeedPerson("4444", "Ana", "Pérez", RoleName.MEMBER),
            new SeedPerson("4445", "Santiago", "Herrera", RoleName.MEMBER),
            new SeedPerson("4446", "Valentina", "Ochoa", RoleName.MEMBER),
            new SeedPerson("4447", "Sebastián", "Londoño", RoleName.MEMBER),
            new SeedPerson("4448", "Camila", "Restrepo", RoleName.MEMBER),

            // Casos raros, que son los que rompen cosas.

            // Sin correo: comprueba que una cuenta puede existir identificada solo por documento.
            new SeedPerson("5555", "Kiosko", "Sin Correo", false, RoleName.MEMBER),

            // El caso real de producción: una sola persona que administra y además dicta. Sirve para
            // comprobar que NO hay que cambiar de rol ni volver a entrar — el token trae las dos
            // autoridades a la vez y los endpoints de ambos roles responden en la misma sesión.
            new SeedPerson("9999", "Elena", "Vargas", RoleName.ADMIN, RoleName.TEACHER));

    /** Lunes a viernes, una franja por día. Suficiente para que la agenda no salga vacía. */
    private static final List<int[]> SCHEDULES = List.of(
            new int[]{1, 18, 0, 19, 0},
            new int[]{2, 18, 0, 19, 0},
            new int[]{3, 18, 0, 19, 0},
            new int[]{4, 19, 0, 20, 0},
            new int[]{5, 19, 0, 20, 0});

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.warn("app.seed.enabled=true -> sembrando datos de prueba con contraseñas conocidas");

        PEOPLE.forEach(this::seedPerson);
        seedTerminal();
        SCHEDULES.forEach(this::seedSchedule);

        log.warn("Datos de prueba listos. La contraseña de cada cuenta es su número de documento.");
    }

    /** Cuenta técnica exclusiva para el equipo físico de entrada; no tiene persona asociada. */
    private void seedTerminal() {
        final String email = "terminal@psraerobicos.com";
        if (userRepository.existsByEmail(email)) {
            log.debug("La cuenta terminal ya existe; el seed no la toca");
            return;
        }

        User terminal = userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Terminal2026!"))
                .active(true)
                .build());
        userRoleProcedureRepository.assign(terminal.getId(), RoleName.PSR_TERMINAL);
        log.warn("Cuenta de terminal creada: {}", email);
    }

    /**
     * Crea persona + cuenta por los mismos procedimientos que usa el alta real, así que si el seed
     * funciona, el alta también.
     * <p>
     * La contraseña es el número de documento, igual que en el alta real cuando no se envía una
     * (RF-01). No hace falta recordar una contraseña distinta por cuenta.
     */
    private void seedPerson(SeedPerson seed) {
        if (personRepository.existsByDocumentNumber(seed.documentNumber())) {
            log.debug("La persona {} ya existe; el seed no la toca", seed.documentNumber());
            return;
        }

        Integer personId = personProcedureRepository.registerPerson(
                DocumentType.CC, seed.documentNumber(), seed.firstName(), seed.lastName(),
                "3000000000", birthDateFor(seed), RegistrationSource.ADMIN);

        String email = emailFor(seed);

        Integer userId = userProcedureRepository.registerUserForPerson(
                personId, passwordEncoder.encode(seed.documentNumber()), email);

        // El procedimiento ya dejó el rol MIEMBRO. Se asignan los pedidos y, si MIEMBRO no era uno de
        // ellos, se quita, para que cada cuenta tenga exactamente los roles de su columna y probar
        // permisos no sea ambiguo. El orden importa: sp_quitar_rol se niega a dejar a un usuario sin
        // ningún rol, así que primero se asigna y después se quita.
        List<RoleName> wanted = List.of(seed.roles());
        wanted.stream()
                .filter(role -> role != RoleName.MEMBER)
                .forEach(role -> userRoleProcedureRepository.assign(userId, role));

        if (!wanted.contains(RoleName.MEMBER)) {
            userRoleProcedureRepository.revoke(userId, RoleName.MEMBER);
        }

        log.info("Cuenta de prueba creada: {} / {} {}", seed.documentNumber(),
                email != null ? email : "sin correo", wanted);
    }

    /**
     * Correo derivado del nombre: Miguel Martínez → miguelmartinez@psr.local. Es el que uno escribiría
     * a mano, y se adivina viendo la lista sin tener que ir a buscarlo.
     * <p>
     * Se le quitan las tildes porque un correo con "í" es válido en el estándar pero un incordio de
     * teclear, y aquí el objetivo es justo lo contrario. Ojo: esto es solo para el dato sembrado — la
     * BÚSQUEDA sí tiene que aguantar tildes, y por eso los nombres las conservan.
     * <p>
     * No puede haber choques con el índice único de correo mientras no se repita un nombre completo
     * en la lista de arriba, que es fácil de vigilar a ojo con veintidós filas.
     */
    private String emailFor(SeedPerson seed) {
        if (!seed.withEmail()) {
            return null;
        }

        return stripAccents(seed.firstName() + seed.lastName()).toLowerCase() + "@psr.local";
    }

    /**
     * Fecha de nacimiento derivada del número de documento, no aleatoria: el mismo seed produce
     * siempre la misma fecha, así que reiniciar la aplicación no cambia los datos ni ensucia las
     * comparaciones entre pruebas.
     * <p>
     * Reparte los años entre 1975 y 2004 para que haya variedad de edades sin escribir veintidós
     * fechas a mano. El día se corta en 28 para no tener que pensar en febrero.
     */
    private static LocalDate birthDateFor(SeedPerson seed) {
        int n = Math.abs(seed.documentNumber().hashCode());
        return LocalDate.of(1975 + (n % 30), 1 + (n % 12), 1 + (n % 28));
    }

    /** Descompone cada letra acentuada en letra + tilde y descarta las tildes. */
    private static String stripAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", "");
    }

    /**
     * El horario se identifica por día + hora de inicio, que es lo que lo hace único en la práctica:
     * sin este control, cada reinicio añadiría otro cuadro idéntico encima del anterior.
     */
    private void seedSchedule(int[] seed) {
        int dayOfWeek = seed[0];
        LocalTime startTime = LocalTime.of(seed[1], seed[2]);
        LocalTime endTime = LocalTime.of(seed[3], seed[4]);

        boolean exists = scheduleRepository
                .findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(dayOfWeek).stream()
                .anyMatch(existing -> existing.getStartTime().equals(startTime));

        if (exists) {
            log.debug("El horario del día {} a las {} ya existe; el seed no lo toca", dayOfWeek, startTime);
            return;
        }

        scheduleRepository.save(Schedule.builder()
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .endTime(endTime)
                .active(true)
                .build());
        log.info("Horario de prueba creado: día {} de {} a {}", dayOfWeek, startTime, endTime);
    }
}
