package com.jhanp.psraerobicsbackend.config;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserProcedureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Siembra una semana de operacion ya transcurrida: cinco dias habiles de clases con sus asistentes,
 * su dinero y sus casos raros.
 * <p>
 * <b>Para que.</b> {@link TestDataSeeder} deja cuentas con las que entrar, y nada mas: una base
 * recien migrada no tiene ni una asistencia, asi que los reportes salen vacios, los historiales no
 * tienen que mostrar y todos los saldos son cero. Revisar esas pantallas obligaba a fabricar datos a
 * mano cada vez, y a mano siempre falta justo el caso que rompe.
 * <p>
 * <b>Fechas relativas a hoy, nunca fijas.</b> Todo se ancla a hoy menos N dias habiles. Con fechas
 * fijas el seed envejece: a las dos semanas cualquier reporte "de esta semana" vuelve a salir vacio y
 * hay que regenerarlo. Los sabados y domingos se saltan porque no hay clase.
 * <p>
 * <b>Escribe con INSERT directo y no por los procedimientos.</b> No es un atajo: sp_registrar_llegada
 * y sp_registrar_asistencia estan escritos para el momento presente — resuelven la sesion con
 * fn_obtener_o_crear_sesion_del_momento y fechan con now(). Sirven para el flujo real y no para
 * reconstruir el martes pasado. Lo que si se respeta es todo lo que la base exige: los CHECK de
 * movimiento_cuenta, el unico de llegada por (persona, sesion) y los tipos enumerados. Ninguna fila
 * de aqui podria haberse creado por la API, pero todas podrian haber existido.
 * <p>
 * <b>Opt-in y separado del otro seed</b> ({@code app.seed.historical.enabled}), porque son dos cosas
 * distintas: las cuentas de prueba se quieren casi siempre, y ciento cincuenta asistencias inventadas
 * solo en el entorno de pruebas. Fuera de ahi, los dos apagados.
 * <p>
 * Es idempotente por la via mas simple: si la primera persona de su lote ya existe, no hace nada.
 * Para recargar hay que limpiar a mano — ver el javadoc de {@link #alreadySeeded()}.
 *
 * @see TestDataSeeder las cuentas por rol, que este da por hechas
 */
@Component
@ConditionalOnProperty(name = "app.seed.historical.enabled", havingValue = "true")
// Despues de TestDataSeeder: necesita sus cuentas de admin y profesor para poder atribuir cada
// asistencia y cada movimiento a alguien.
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class HistoricalDataSeeder implements ApplicationRunner {

    /** Dias habiles ya transcurridos que se generan. */
    private static final int BUSINESS_DAYS = 5;

    /** Asistentes confirmados por sesion. */
    private static final int ATTENDEES_PER_SESSION = 30;

    /** Llegadas que se quedan sin confirmar en cada sesion pasada. */
    private static final int UNCONFIRMED_PER_SESSION = 4;

    /**
     * Personas del lote. Mas que asistentes por sesion a proposito: con exactamente treinta, las
     * mismas treinta irian todos los dias y la asistencia no tendria ninguna variedad que mirar.
     */
    private static final int PEOPLE = 45;

    /** Primer documento del lote. El rango 70001+ no se cruza con los de TestDataSeeder (1111-9999). */
    private static final int FIRST_DOCUMENT = 70001;

    /** Lo que cuesta una clase en el historico. Coincide con la tarifa inicial de V1. */
    private static final BigDecimal FEE = new BigDecimal("4000");

    // La franja real de PSR, la misma que siembra ProductionAccountsSeeder. Si no coincidieran, las
    // sesiones del historico quedarian a una hora y el horario al que las ata createSession a otra.
    private static final LocalTime CLASS_START = LocalTime.of(19, 0);
    private static final LocalTime CLASS_END = LocalTime.of(20, 0);

    private final JdbcTemplate jdbc;
    private final PersonProcedureRepository personProcedureRepository;
    private final UserProcedureRepository userProcedureRepository;
    private final PasswordEncoder passwordEncoder;

    /** Ids resueltos al arrancar, para no consultarlos en cada fila. */
    private Integer adminUserId;
    private Integer teacherUserId;
    private Integer cashMethodId;
    private Integer transferMethodId;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (alreadySeeded()) {
            log.info("El historico de prueba ya esta sembrado; no se toca nada");
            return;
        }

        if (!resolveActors()) {
            log.warn("No hay cuentas de admin o profesor: el historico no se siembra. "
                    + "Arranca antes con app.seed.test.enabled=true");
            return;
        }

        seedInactivePaymentMethod();

        List<Integer> people = seedPeople();
        List<LocalDate> days = businessDaysBack(BUSINESS_DAYS);

        int attendances = 0;
        for (LocalDate day : days) {
            attendances += seedPastSession(day, people);
        }

        seedTodaySession(people);
        seedCancelledSessionWithArrivals(people);
        seedScheduleException();
        seedEdgeCases(people);

        log.warn("Historico de prueba sembrado: {} personas, {} dias habiles, {} asistencias. "
                        + "Entra con cualquier documento del rango {}-{} (la contrasena es el documento).",
                people.size(), days.size(), attendances, FIRST_DOCUMENT, FIRST_DOCUMENT + PEOPLE - 1);
    }

    /**
     * La marca es la primera persona del lote. Basta con una: o se sembro entero o no se sembro, porque
     * todo ocurre en la misma transaccion.
     * <p>
     * Para recargar desde cero hay que borrar a mano, y el orden importa por las claves foraneas:
     * asistencia, movimiento_cuenta, llegada, sesion, usuario_rol, usuario, persona — filtrando por
     * los documentos del rango de este seed.
     */
    private boolean alreadySeeded() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM persona WHERE numero_documento = ?",
                Integer.class, String.valueOf(FIRST_DOCUMENT));

        return count != null && count > 0;
    }

    /**
     * Quien queda como autor de cada asistencia y cada movimiento. La base lo exige: asistencia.
     * marcado_por y movimiento_cuenta.usuario_id son NOT NULL con clave foranea a usuario.
     *
     * @return false si faltan las cuentas, en cuyo caso no se siembra nada
     */
    private boolean resolveActors() {
        adminUserId = firstUserWithRole("ADMIN");
        teacherUserId = firstUserWithRole("PROFESOR");

        if (teacherUserId == null) {
            // Un profesor es deseable (la sesion queda con nombre en los reportes) pero no
            // imprescindible: sesion.profesor_usuario_id es nullable.
            teacherUserId = adminUserId;
        }

        cashMethodId = paymentMethodIdByName("EFECTIVO");
        transferMethodId = paymentMethodIdByName("TRANSFERENCIA");

        return adminUserId != null && cashMethodId != null && transferMethodId != null;
    }

    private Integer firstUserWithRole(String roleName) {
        List<Integer> ids = jdbc.queryForList("""
                SELECT u.id
                FROM usuario u
                         JOIN usuario_rol ur ON ur.usuario_id = u.id
                         JOIN rol r ON r.id = ur.rol_id
                WHERE r.nombre = ?::rol_nombre_enum
                  AND u.activo = true
                  AND u.persona_id IS NOT NULL
                ORDER BY u.id
                LIMIT 1
                """, Integer.class, roleName);

        return ids.isEmpty() ? null : ids.get(0);
    }

    private Integer paymentMethodIdByName(String name) {
        List<Integer> ids = jdbc.queryForList(
                "SELECT id FROM metodo_pago WHERE nombre = ?", Integer.class, name);

        return ids.isEmpty() ? null : ids.get(0);
    }

    /**
     * Un tercer metodo de pago, desactivado. Existe para poder comprobar que no aparece en el selector
     * de cobro y que el servidor rechaza un movimiento que lo use: sin un metodo apagado en la base,
     * esa regla no se puede probar sin ir a desactivar uno a mano primero.
     */
    private void seedInactivePaymentMethod() {
        jdbc.update("""
                INSERT INTO metodo_pago (nombre, activo)
                VALUES ('BONO REGALO', false)
                ON CONFLICT (nombre) DO NOTHING
                """);
    }

    /**
     * Las personas del lote. Todas con rol MIEMBRO (se lo pone sp_registrar_usuario_para_persona) y
     * con la contrasena igual al documento, igual que el resto de cuentas de prueba.
     * <p>
     * Telefonos distintos entre si, derivados del documento: con el mismo numero repetido, cualquier
     * alta o edicion de prueba choca contra el aviso de telefono duplicado.
     */
    private List<Integer> seedPeople() {
        List<Integer> personIds = new ArrayList<>(PEOPLE);

        for (int i = 0; i < PEOPLE; i++) {
            String document = String.valueOf(FIRST_DOCUMENT + i);

            Integer personId = personProcedureRepository.registerPerson(
                    DocumentType.CC, document, FIRST_NAMES[i % FIRST_NAMES.length],
                    LAST_NAMES[i % LAST_NAMES.length], "301" + document,
                    LocalDate.of(1980 + (i % 25), 1 + (i % 12), 1 + (i % 28)),
                    i % 3 == 0 ? RegistrationSource.SELF_REGISTRATION : RegistrationSource.ADMIN);

            // Una de cada cinco sin correo: es el caso del kiosko, y conviene que el historico lo
            // tenga para que las pantallas que muestran correo no se prueben siempre con el feliz.
            String email = i % 5 == 0 ? null : "hist" + document + "@psr.local";

            userProcedureRepository.registerUserForPerson(
                    personId, passwordEncoder.encode(document), email);

            personIds.add(personId);
        }

        return personIds;
    }

    private static final String[] FIRST_NAMES = {
            "Luisa", "Mateo", "Sofia", "Daniel", "Valeria", "Nicolas", "Isabella", "Samuel",
            "Mariana", "Emiliano", "Gabriela", "Alejandro", "Antonia", "Martin", "Renata"};

    private static final String[] LAST_NAMES = {
            "Cardenas", "Villalba", "Mejia", "Escobar", "Duarte", "Pineda", "Acosta",
            "Moreno", "Castano", "Vergara", "Ibarra", "Cortes", "Bedoya", "Rincon", "Suarez"};

    /**
     * Los ultimos N dias habiles, del mas antiguo al mas reciente. Hoy no entra: estos son dias que ya
     * terminaron, con su sesion cerrada.
     */
    private List<LocalDate> businessDaysBack(int count) {
        List<LocalDate> days = new ArrayList<>(count);
        LocalDate cursor = LocalDate.now().minusDays(1);

        while (days.size() < count) {
            if (cursor.getDayOfWeek().getValue() <= 5) {
                days.add(cursor);
            }
            cursor = cursor.minusDays(1);
        }

        return days.reversed();
    }

    /**
     * Un dia completo: la sesion cerrada, sus asistentes confirmados con su dinero, y unas cuantas
     * llegadas que nadie llego a confirmar.
     * <p>
     * Quienes asisten rotan con el dia, para que no sean siempre los mismos treinta de los cuarenta y
     * cinco. Y cada uno paga de una forma distinta segun su indice — ver {@link #seedPayment}.
     *
     * @return cuantas asistencias se confirmaron
     */
    private int seedPastSession(LocalDate day, List<Integer> people) {
        Integer sessionId = createSession(day, true);
        int offset = (int) (day.toEpochDay() % people.size());
        int confirmed = 0;

        for (int i = 0; i < ATTENDEES_PER_SESSION; i++) {
            Integer personId = people.get((offset + i) % people.size());
            OffsetDateTime arrival = at(day, CLASS_START.minusMinutes(15).plusMinutes(i % 40));

            Integer checkinId = insertArrival(personId, sessionId, arrival, i);
            insertAttendance(checkinId, arrival.plusMinutes(3));
            seedPayment(personId, checkinId, arrival, i);

            confirmed++;
        }

        // Llegadas sin confirmar: gente que entro a la fila y se fue, o que el mostrador no alcanzo a
        // marcar. Es un estado normal y las pantallas tienen que saber mostrarlo.
        for (int i = 0; i < UNCONFIRMED_PER_SESSION; i++) {
            Integer personId = people.get((offset + ATTENDEES_PER_SESSION + i) % people.size());
            insertArrival(personId, sessionId, at(day, CLASS_START.plusMinutes(20 + i)), i);
        }

        return confirmed;
    }

    /**
     * Cuatro formas de pagar, repartidas por el indice del asistente. La variedad no es decorativa:
     * cada una produce un estado de cuenta distinto, y son justo los estados que las pantallas de
     * saldo, vueltos y reembolsos tienen que saber pintar.
     * <ul>
     *   <li>La mayoria paga justo el dia: recibe y abona la tarifa, saldo cero.</li>
     *   <li>Uno de cada siete paga con vuelto pendiente (recibio mas de lo que abono).</li>
     *   <li>Uno de cada once paga por transferencia, sin vuelto posible.</li>
     *   <li>Uno de cada trece no paga: queda debiendo, y su saldo se va a negativo.</li>
     * </ul>
     */
    private void seedPayment(Integer personId, Integer checkinId, OffsetDateTime when, int index) {
        if (index % 13 == 0) {
            return;
        }

        if (index % 7 == 0) {
            // Recibe 5000 por una clase de 4000: quedan 1000 de vuelto. Los pares se entregan y los
            // impares no, para que vista_vueltos_pendientes tenga filas.
            insertMovement(personId, checkinId, new BigDecimal("5000"), FEE, cashMethodId,
                    when, "Cobro de entrada", index % 14 == 0);
            return;
        }

        Integer method = index % 11 == 0 ? transferMethodId : cashMethodId;
        insertMovement(personId, checkinId, FEE, FEE, method, when, "Cobro de entrada", true);
    }

    /**
     * La sesion de hoy, abierta: unas llegadas ya confirmadas y otras esperando en la fila. Es lo que
     * hace que la pantalla de cola tenga algo que mostrar nada mas entrar.
     */
    private void seedTodaySession(List<Integer> people) {
        LocalDate today = LocalDate.now();
        Integer sessionId = createSession(today, false);

        for (int i = 0; i < 12; i++) {
            Integer personId = people.get(i);
            OffsetDateTime arrival = OffsetDateTime.now().minusMinutes(40L - i * 3);

            Integer checkinId = insertArrival(personId, sessionId, arrival, i);

            // Los primeros seis ya pasaron por mostrador; los otros seis siguen en la fila, que es lo
            // que se quiere ver en la pantalla de hoy.
            if (i < 6) {
                insertAttendance(checkinId, arrival.plusMinutes(2));
                seedPayment(personId, checkinId, arrival, i);
            }
        }
    }

    /**
     * Una segunda sesion de hoy, cancelada, con gente que ya habia llegado.
     * <p>
     * Es exactamente el caso que se reporto el 19-sep: las tres vistas de cola filtran por NOT
     * cancelada, asi que estos check-ins existen en la base y no salen en ninguna fila. Sirve para
     * comprobar que la pantalla no los muestra — y que el aviso por WebSocket llega cuando la
     * cancelacion ocurre con la pantalla abierta.
     * <p>
     * horario_id en null: es una sesion suelta, asi no choca con el unico de (horario, fecha) de la
     * sesion de hoy que se acaba de crear.
     */
    private void seedCancelledSessionWithArrivals(List<Integer> people) {
        LocalDate today = LocalDate.now();

        Integer sessionId = jdbc.queryForObject("""
                INSERT INTO sesion (horario_id, fecha, hora_inicio, hora_fin, profesor_usuario_id, cancelada)
                VALUES (NULL, ?, ?, ?, ?, true)
                RETURNING id
                """, Integer.class, today, at(today, LocalTime.of(6, 0)), at(today, LocalTime.of(7, 0)),
                teacherUserId);

        for (int i = 0; i < 3; i++) {
            insertArrival(people.get(PEOPLE - 1 - i), sessionId,
                    at(today, LocalTime.of(5, 50).plusMinutes(i)), i);
        }
    }

    /**
     * Una excepcion de horario en el proximo dia que coincida con el dia de la semana de algun horario
     * activo. La fecha tiene que caer en ese dia o trg_validar_dia_excepcion lo rechaza — que es
     * justamente lo que se quiere poder probar.
     */
    private void seedScheduleException() {
        List<Integer> days = jdbc.queryForList(
                "SELECT dia_semana FROM horario WHERE activo = true ORDER BY dia_semana LIMIT 1",
                Integer.class);

        if (days.isEmpty()) {
            log.debug("Sin horarios activos: no se siembra ninguna excepcion de horario");
            return;
        }

        int dayOfWeek = days.get(0);
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek().getValue() != dayOfWeek) {
            date = date.plusDays(1);
        }

        jdbc.update("""
                INSERT INTO horario_excepcion (horario_id, fecha, motivo)
                SELECT id, ?, 'Festivo de prueba (seed historico)'
                FROM horario WHERE activo = true AND dia_semana = ?
                ORDER BY id LIMIT 1
                ON CONFLICT (horario_id, fecha) DO NOTHING
                """, date, dayOfWeek);
    }

    /**
     * Los estados de cuenta que hay que poder mirar uno por uno, cada uno en su propia persona para
     * que no se estorben entre si. Se usan las ultimas del lote, que no entran en la rotacion de
     * asistentes de los dias pasados.
     */
    private void seedEdgeCases(List<Integer> people) {
        LocalDate day = LocalDate.now().minusDays(2);
        OffsetDateTime when = at(day, CLASS_START);

        // 1) El caso exacto del reporte del 19-sep: abono 15.000, consumio 10.000, saldo 5.000.
        //    Antes de la correccion, aqui reembolsable decia 15.000 y devolverlos dejaba la cuenta
        //    debiendo. Con fn_saldo_reembolsable tiene que decir 5.000.
        Integer person = people.get(PEOPLE - 5);
        insertMovement(person, null, new BigDecimal("15000"), new BigDecimal("15000"),
                cashMethodId, when, "Abono por adelantado", true);
        insertStandaloneAttendance(person, day, new BigDecimal("5000"));
        insertStandaloneAttendance(person, day.minusDays(1), new BigDecimal("5000"));

        // 2) Saldo a favor alto y sin consumir: el tope del reembolso es todo lo abonado.
        insertMovement(people.get(PEOPLE - 4), null, new BigDecimal("80000"), new BigDecimal("80000"),
                transferMethodId, when, "Abono de 20 dias", true);

        // 3) Debe: asistio sin pagar, el saldo queda negativo y no hay nada que devolverle.
        insertStandaloneAttendance(people.get(PEOPLE - 3), day, FEE);
        insertStandaloneAttendance(people.get(PEOPLE - 3), day.minusDays(1), FEE);

        // 4) Con un reembolso parcial ya hecho: el abono sigue vivo por lo que queda.
        Integer refunded = people.get(PEOPLE - 2);
        Integer movementId = insertMovement(refunded, null, new BigDecimal("40000"),
                new BigDecimal("40000"), cashMethodId, when, "Abono de 10 dias", true);
        insertRefund(refunded, movementId, new BigDecimal("16000"), when.plusDays(1),
                "Devolucion parcial: se va de viaje");

        // 5) Saldo exactamente en cero: abono un dia y lo consumio. Es el borde que suele fallar.
        Integer even = people.get(PEOPLE - 1);
        insertMovement(even, null, FEE, FEE, cashMethodId, when, "Abono de un dia", true);
        insertStandaloneAttendance(even, day, FEE);
    }

    // --- Escritura ------------------------------------------------------------------------------

    /**
     * La sesion de un dia, colgada del horario que corresponda a ese dia de la semana si lo hay.
     * <p>
     * El horario importa para los reportes — sin el, la sesion no pertenece a ninguna franja—, pero
     * uq_sesion_horario_fecha solo admite una por (horario, fecha), asi que si ya existe una se
     * reutiliza en vez de fallar. Eso pasa de verdad: el scheduler de creacion anticipada ya genero
     * las sesiones de los proximos dias antes de que este seed corra.
     *
     * @param closed true para dejarla cerrada, que es lo que corresponde a un dia que ya termino
     */
    private Integer createSession(LocalDate day, boolean closed) {
        List<Integer> existing = jdbc.queryForList(
                "SELECT id FROM sesion WHERE fecha = ? AND NOT cancelada ORDER BY id LIMIT 1",
                Integer.class, day);

        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        List<Integer> scheduleIds = jdbc.queryForList("""
                SELECT h.id
                FROM horario h
                WHERE h.dia_semana = ?
                  AND NOT EXISTS (SELECT 1 FROM sesion s WHERE s.horario_id = h.id AND s.fecha = ?)
                ORDER BY h.id
                LIMIT 1
                """, Integer.class, day.getDayOfWeek().getValue(), day);

        Integer scheduleId = scheduleIds.isEmpty() ? null : scheduleIds.get(0);

        return jdbc.queryForObject("""
                INSERT INTO sesion (horario_id, fecha, hora_inicio, hora_fin, profesor_usuario_id, cancelada)
                VALUES (?, ?, ?, ?, ?, false)
                RETURNING id
                """, Integer.class, scheduleId, day, at(day, CLASS_START),
                closed ? at(day, CLASS_END) : null, teacherUserId);
    }

    /**
     * El metodo de check-in rota entre los tres: con uno solo, el conversor de metodo_checkin_enum se
     * probaria siempre por el mismo camino — y ya hubo un fallo que solo aparecia con DOCUMENTO y
     * MANUAL, porque QR es el unico cuyo nombre coincide en los dos idiomas.
     */
    private Integer insertArrival(Integer personId, Integer sessionId, OffsetDateTime when, int index) {
        return jdbc.queryForObject("""
                INSERT INTO llegada (persona_id, sesion_id, hora_llegada, metodo_checkin)
                VALUES (?, ?, ?, ?::metodo_checkin_enum)
                RETURNING id
                """, Integer.class, personId, sessionId, when, CHECKIN_METHODS[index % 3]);
    }

    private static final String[] CHECKIN_METHODS = {"DOCUMENTO", "QR", "MANUAL"};

    private void insertAttendance(Integer checkinId, OffsetDateTime when) {
        jdbc.update("""
                INSERT INTO asistencia (llegada_id, monto_cobrado, marcado_por, hora_marcado)
                VALUES (?, ?, ?, ?)
                """, checkinId, FEE, adminUserId, when);
    }

    /**
     * Una asistencia con su llegada propia, en una sesion suelta de ese dia. Para los casos limite,
     * donde lo que importa es el efecto sobre el saldo y no la sesion en la que ocurrio.
     * <p>
     * El monto va como parametro: el historico puede tener cobros distintos de la tarifa de hoy, que
     * es justo lo que pasa cuando el precio cambia con el tiempo.
     */
    private void insertStandaloneAttendance(Integer personId, LocalDate day, BigDecimal amount) {
        Integer sessionId = jdbc.queryForObject("""
                INSERT INTO sesion (horario_id, fecha, hora_inicio, hora_fin, profesor_usuario_id, cancelada)
                VALUES (NULL, ?, ?, ?, ?, false)
                RETURNING id
                """, Integer.class, day, at(day, LocalTime.of(7, 0)), at(day, LocalTime.of(8, 0)),
                teacherUserId);

        Integer checkinId = insertArrival(personId, sessionId, at(day, LocalTime.of(7, 5)), 0);

        jdbc.update("""
                INSERT INTO asistencia (llegada_id, monto_cobrado, marcado_por, hora_marcado)
                VALUES (?, ?, ?, ?)
                """, checkinId, amount, adminUserId, at(day, LocalTime.of(7, 10)));
    }

    /**
     * Un ABONO. Los CHECK de la tabla exigen recibido >= abonado >= 0, y la diferencia es el vuelto:
     * si no se entrega, la fila aparece en vista_vueltos_pendientes.
     *
     * @param checkinId null para un pago por adelantado, que no cuelga de ninguna llegada
     * @return el id del movimiento, para poder reembolsarlo despues
     */
    private Integer insertMovement(Integer personId, Integer checkinId, BigDecimal received,
                                   BigDecimal credited, Integer methodId, OffsetDateTime when,
                                   String note, boolean changeDelivered) {
        return jdbc.queryForObject("""
                INSERT INTO movimiento_cuenta
                    (persona_id, llegada_id, monto_recibido, monto_abonado, metodo_pago_id,
                     tipo_movimiento, vuelto_entregado, fecha_hora, usuario_id, nota)
                VALUES (?, ?, ?, ?, ?, 'ABONO', ?, ?, ?, ?)
                RETURNING id
                """, Integer.class, personId, checkinId, received, credited, methodId,
                changeDelivered, when, adminUserId, note);
    }

    /**
     * Un REEMBOLSO contra un abono. El CHECK de la tabla exige recibido = 0 y abonado negativo: el
     * signo es lo que hace que reste del saldo al sumarse con los demas movimientos.
     * <p>
     * Hereda el llegada_id del abono original, igual que hace sp_registrar_reembolso, para que la nota
     * de la fila refleje lo ultimo que paso.
     */
    private void insertRefund(Integer personId, Integer originalMovementId, BigDecimal amount,
                              OffsetDateTime when, String note) {
        jdbc.update("""
                INSERT INTO movimiento_cuenta
                    (persona_id, llegada_id, monto_recibido, monto_abonado, metodo_pago_id,
                     tipo_movimiento, movimiento_original_id, vuelto_entregado, fecha_hora, usuario_id, nota)
                SELECT ?, o.llegada_id, 0, ?, o.metodo_pago_id, 'REEMBOLSO', o.id, true, ?, ?, ?
                FROM movimiento_cuenta o
                WHERE o.id = ?
                """, personId, amount.negate(), when, adminUserId, note, originalMovementId);
    }

    /** Ancla una hora local a la zona del sistema, la misma con la que la base evalua now(). */
    private OffsetDateTime at(LocalDate day, LocalTime time) {
        return day.atTime(time).atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
