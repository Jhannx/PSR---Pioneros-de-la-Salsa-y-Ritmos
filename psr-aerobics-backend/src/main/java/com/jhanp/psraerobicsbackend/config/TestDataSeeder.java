package com.jhanp.psraerobicsbackend.config;

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
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;

/**
 * Siembra datos de prueba al arrancar: cinco cuentas por rol, los casos raros que conviene tener
 * siempre a mano y la cuenta técnica de la terminal.
 * <p>
 * <b>Ya no siembra horarios.</b> Se fueron a {@link ProductionAccountsSeeder} el 28-09-2026: eran el
 * único dato de operación real que vivía aquí, y eso ataba la agenda a una bandera que en producción
 * tiene que estar apagada. Tenerlos en los dos sitios tampoco valía: los de aquí eran de lunes a
 * viernes a las 18:00 y los reales son de lunes a jueves a las 19:00, así que en local habrían
 * convivido dos agendas distintas y ninguna prueba se habría parecido a producción.
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
 * Es idempotente: si la persona ya existe, no la toca. Se puede reiniciar sin miedo.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
// Antes que HistoricalDataSeeder, que necesita estas cuentas para atribuirle a alguien cada
// asistencia y cada movimiento del historico.
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class TestDataSeeder implements ApplicationRunner {

    private final PersonRepository personRepository;
    private final PersonProcedureRepository personProcedureRepository;
    private final UserProcedureRepository userProcedureRepository;
    private final UserRepository userRepository;
    private final UserRoleProcedureRepository userRoleProcedureRepository;
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

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.warn("app.seed.enabled=true -> sembrando datos de prueba con contraseñas conocidas");

        PEOPLE.forEach(this::seedPerson);
        seedTerminal();

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
                phoneFor(seed), birthDateFor(seed), RegistrationSource.ADMIN);

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

    /**
     * Teléfono derivado del documento, y por eso distinto en cada persona sembrada.
     * <p>
     * Antes era el literal "3000000000" para las veintidós, y eso convertía una base recién sembrada
     * en un campo de minas: {@code persona.telefono} no tiene UNIQUE, pero el alta y la edición avisan
     * con un 409 cuando el número ya está en otra ficha, así que <b>cualquier</b> persona de prueba
     * chocaba con todas las demás. El aviso era cierto y aun así parecía un fallo de la aplicación.
     * <p>
     * Diez dígitos y prefijo 300 para que se parezca a un móvil colombiano real.
     */
    private static String phoneFor(SeedPerson seed) {
        String digits = seed.documentNumber().replaceAll("\\D", "");
        String tail = digits.length() > 7 ? digits.substring(digits.length() - 7) : digits;

        return "300" + "0".repeat(7 - tail.length()) + tail;
    }

    /** Descompone cada letra acentuada en letra + tilde y descarta las tildes. */
    private static String stripAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", "");
    }
}
