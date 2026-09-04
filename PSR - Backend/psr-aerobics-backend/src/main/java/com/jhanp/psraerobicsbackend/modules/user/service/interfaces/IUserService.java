package com.jhanp.psraerobicsbackend.modules.user.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.role.dto.request.UpdateRolesRequest;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.TeacherResponse;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IUserService {


    UserResponse findById(Integer id);

    UserResponse findByPersonId(Integer personId);

    // Búsqueda de login: acepta documento o correo.
    UserResponse findByIdentifier(String identifier);

    Page<UserResponse> findAll(Pageable pageable);

    /**
     * Usuarios activos con rol PROFESOR: son los asignables a una sesión. Devuelve TeacherResponse
     * porque el selector necesita el nombre completo y no debe ver el resto de la cuenta.
     */
    List<TeacherResponse> findAssignableTeachers();

    UserResponse createForAdmin(Integer personId, String newPassword);

    UserResponse createForMember(Integer personId, String newPassword);

    void changePassword(Integer id, String newPassword);

    UserResponse updateRoles(Integer id, UpdateRolesRequest request);

    UserResponse setActive(Integer id, boolean active);

    Boolean deleteUser(Integer id);

}
