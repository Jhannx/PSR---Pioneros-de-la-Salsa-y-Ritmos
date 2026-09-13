package com.jhanp.psraerobicsbackend.modules.user.service;

import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.TeacherResponse;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.mapper.UserMapper;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.modules.user.service.interfaces.IUserService;
import com.jhanp.psraerobicsbackend.shared.exception.DuplicateResourceException;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.validation.SearchTerms;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements IUserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserProcedureRepository userProcedureRepository;
    private final UserMapper userMapper;



    /**
     * El hash se calcula aquí y el procedimiento lo recibe ya hecho: la contraseña en claro nunca
     * llega a la base de datos. Después del CALL hay que releer la fila, porque el procedimiento
     * solo devuelve el id por OUT.
     */
    @Override
    @Transactional
    public UserResponse createForPerson(Integer personId, String rawPassword, String email) {
        String normalizedEmail = normalizeEmail(email);

        // Se comprueba antes del CALL para responder 409 con un mensaje claro; sin esto, la
        // restricción UNIQUE de usuario.correo saltaría como un error genérico de base de datos.
        if (normalizedEmail != null && userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Ya existe una cuenta con el correo " + normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        Integer userId = userProcedureRepository.registerUserForPerson(personId, passwordHash, normalizedEmail);

        return userMapper.toResponse(findUserOrThrow(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Integer id) {
        return userMapper.toResponse(findUserOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByPersonId(Integer personId) {
        return userRepository.findByPersonId(personId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La persona " + personId + " no tiene cuenta de acceso"));
    }

    /**
     * El identificador es el mismo campo para las dos columnas: una cuenta se creó con documento o
     * con correo, y quien inicia sesión escribe el que tenga.
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse findByIdentifier(String identifier) {
        return userRepository.findByDocumentNumberOrEmail(identifier, identifier)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una cuenta con el identificador " + identifier));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> search(String term, Pageable pageable) {
        // unsorted() no es opcional: la consulta es nativa y trae su propio ORDER BY. Con un
        // ?sort= en la URL, Spring le pega un segundo ORDER BY y el SQL revienta con 500.
        return userRepository.search(term, SearchTerms.unsorted(pageable)).map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponse> findAssignableTeachers() {
        return userRepository.findActiveByRole(RoleName.TEACHER.getDbValue())
                .stream()
                .map(userMapper::toTeacherResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse setActive(Integer id, boolean active) {
        User user = findUserOrThrow(id);
        user.setActive(active);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Integer id, String newPassword) {
        User user = findUserOrThrow(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        userRepository.save(user);
    }

    /**
     * Se exige la contraseña actual aunque quien llama ya esté autenticado: si alguien deja la sesión
     * abierta, no debería bastar con eso para secuestrarle la cuenta cambiándosela.
     */
    @Override
    @Transactional
    public void changeOwnPassword(String identifier, String currentPassword, String newPassword) {
        User user = userRepository.findByDocumentNumberOrEmail(identifier, identifier)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una cuenta con el identificador " + identifier));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.warn("Cambio de contraseña rechazado para la cuenta {}: la actual no coincide", user.getId());
            throw new InvalidDataException("La contraseña actual no es correcta");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Contraseña cambiada por el propio usuario ID {}", user.getId());
    }

    /**
     * La contraseña se lee de la PERSONA y no de la cuenta: usuario.numero_documento es opcional
     * (una cuenta puede haberse creado solo con correo), mientras que persona.numero_documento es
     * NOT NULL. Tomarlo de la cuenta dejaría el reseteo sin valor justo en esos casos.
     */
    @Override
    @Transactional
    public String resetPasswordToDocument(Integer personId) {
        User user = userRepository.findByPersonId(personId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La persona " + personId + " no tiene cuenta de acceso"));

        Person person = user.getPerson();
        String documentNumber = person != null ? person.getDocumentNumber() : null;

        if (documentNumber == null || documentNumber.isBlank()) {
            throw new InvalidDataException(
                    "La persona no tiene número de documento, así que no hay contraseña inicial que restaurar");
        }

        user.setPasswordHash(passwordEncoder.encode(documentNumber));
        userRepository.save(user);

        // El documento no se registra: es la contraseña que acaba de quedar puesta.
        log.info("Contraseña reiniciada al documento para la cuenta ID {} (persona {})", user.getId(), personId);
        return documentNumber;
    }

    private User findUserOrThrow(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta " + id));
    }

    /**
     * El correo se guarda en minúsculas y sin espacios. Sin esto, "Ana@x.com" y "ana@x.com" serían dos
     * cuentas distintas para el UNIQUE, y quien se registrara con mayúsculas no podría entrar
     * escribiéndolo como lo escribe siempre.
     */
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
