package com.jhanp.psraerobicsbackend.modules.user.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.user.dto.response.TeacherResponse;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * La cuenta de acceso: crearla, encontrarla y cambiarle la contraseña. Nada más.
 * <p>
 * No orquesta el alta —de eso se encarga {@code RegisterService}, que decide si viene de admin o de
 * autoregistro y si sale correo— ni gestiona roles, que viven en {@code IUserRoleService}. Aquí solo
 * se toman los datos, se crea el usuario y se hashea.
 */
public interface IUserService {

    /**
     * Crea la cuenta de una persona ya registrada, hasheando la contraseña recibida.
     *
     * @param rawPassword contraseña en claro; el hash se calcula aquí y solo el hash llega a la base
     * @param email       opcional; sin él la cuenta se identifica solo por número de documento
     */
    UserResponse createForPerson(Integer personId, String rawPassword, String email);

    UserResponse findById(Integer id);

    UserResponse findByPersonId(Integer personId);

    // Búsqueda de login: acepta documento o correo.
    UserResponse findByIdentifier(String identifier);

    Page<UserResponse> findAll(Pageable pageable);

    /**
     * Listado filtrado por un único texto libre: nombre de la persona, documento o correo.
     * <p>
     * Mismo criterio que la búsqueda de personas —parcial, sin distinguir mayúsculas ni tildes—
     * porque quien usa las dos pantallas espera que el mismo texto encuentre lo mismo.
     */
    Page<UserResponse> search(String term, Pageable pageable);

    /**
     * Usuarios activos con rol PROFESOR: son los asignables a una sesión. Devuelve TeacherResponse
     * porque el selector necesita el nombre completo y no debe ver el resto de la cuenta.
     */
    List<TeacherResponse> findAssignableTeachers();

    UserResponse setActive(Integer id, boolean active);

    /** Fija una contraseña sin pedir la anterior. La usa el canje de un token de un solo uso. */
    void changePassword(Integer id, String newPassword);

    /**
     * Cambio hecho por el propio dueño, que sí conoce su contraseña actual.
     *
     * @param identifier documento o correo, tal como viene del subject del JWT
     */
    void changeOwnPassword(String identifier, String currentPassword, String newPassword);

    /**
     * Reseteo asistido por el mostrador: la contraseña de la persona vuelve a ser su número de
     * documento, la misma regla que ya rige el alta (RF-01).
     * <p>
     * Existe porque el flujo por correo no le sirve a quien se registró sin correo —el caso normal de
     * la Terminal PSR—, que hoy no tiene ningún camino de recuperación. No hace falta token ni
     * verificación de identidad: la verificación la hace el staff en persona, viendo el documento
     * físico, igual que cuando registró a esa persona por primera vez.
     *
     * @param personId id de PERSONA, no de usuario: es lo que lleva la ruta y lo que ve el mostrador
     * @return el número de documento que quedó como contraseña, para poder decírselo a la persona
     */
    String resetPasswordToDocument(Integer personId);
}
