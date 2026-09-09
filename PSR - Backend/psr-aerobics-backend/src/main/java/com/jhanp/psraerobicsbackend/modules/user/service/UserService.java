package com.jhanp.psraerobicsbackend.modules.user.service;

import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.TeacherResponse;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.mapper.UserMapper;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.modules.user.service.interfaces.IUserService;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
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
    public UserResponse createForPerson(Integer personId, String rawPassword) {
        String passwordHash = passwordEncoder.encode(rawPassword);
        Integer userId = userProcedureRepository.registerUserForPerson(personId, passwordHash);

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
    public List<TeacherResponse> findAssignableTeachers() {
        return userRepository.findActiveByRole(RoleName.TEACHER)
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

    private User findUserOrThrow(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta " + id));
    }
}
