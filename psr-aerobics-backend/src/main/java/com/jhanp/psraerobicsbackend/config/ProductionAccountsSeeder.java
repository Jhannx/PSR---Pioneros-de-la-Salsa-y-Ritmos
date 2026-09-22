package com.jhanp.psraerobicsbackend.config;

import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.role.repository.UserRoleProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuentas que deben existir siempre: el super admin y las cinco terminales PSR.
 * <p>
 * Corre antes que los seeders de prueba ({@code @Order(0)}) y <b>sin bandera condicional</b>: estas
 * cuentas son de producción, no de prueba. Sin el admin no hay forma de entrar al sistema la primera
 * vez; sin las terminales, los equipos físicos de entrada no pueden operar.
 * <p>
 * Idempotente: si la cuenta ya existe (por correo), no la toca. Reiniciar la app veinte veces no
 * cambia nada. Una contraseña cambiada desde la aplicación no se pisa.
 * <p>
 * Ninguna de las seis cuentas lleva persona asociada. El trigger
 * {@code trg_validar_persona_requerida} lo permite para los roles ADMIN y PSR_TERMINAL — son cuentas
 * técnicas, sin nadie detrás. En las pantallas de auditoría aparecen por su correo en vez de por un
 * nombre; {@code vista_historial_cambios} ya lo contempla.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class ProductionAccountsSeeder implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "adminpsraerobicos@gmail.com";
    private static final String ADMIN_PASSWORD = "adminrootpsraerobicos";

    private static final int TERMINAL_COUNT = 5;
    private static final String TERMINAL_EMAIL_PATTERN = "terminal%d@psraerobicos.com";
    private static final String TERMINAL_PASSWORD_PATTERN = "psrterminalaerobicos.%d";

    private final UserRepository userRepository;
    private final UserRoleProcedureRepository userRoleProcedureRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedTerminals();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("La cuenta de admin ya existe ({}); no se toca", ADMIN_EMAIL);
            return;
        }

        User admin = userRepository.saveAndFlush(User.builder()
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .active(true)
                .build());
        userRoleProcedureRepository.assign(admin.getId(), RoleName.ADMIN);

        log.warn("Cuenta de admin de producción creada: {}", ADMIN_EMAIL);
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
}
