package com.jhanp.psraerobicsbackend.modules.person.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonUpdateRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonProfileViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPersonService {

    /**
     * Alta de persona: sp_registrar_persona + sp_registrar_usuario_para_persona en la misma
     * transacción. Toda persona nace con cuenta de acceso y rol MIEMBRO.
     * <p>
     * source va como parámetro y no dentro del request porque lo decide la ruta (ADMIN en
     * /api/personas, AUTOREGISTRO en /api/personas/autoregistro). El hash de la contraseña se
     * calcula en Java antes de la segunda llamada; si el request no trae password, se hashea el
     * número de documento.
     * <p>
     * Los procedimientos solo devuelven ids: registeredAt y active salen de releer la fila
     * (patrón H).
     */
    PersonResponse create(PersonRequest request, RegistrationSource source);

    /**
     * Edición de datos básicos por JPA. No toca source, registeredAt, active ni la contraseña.
     */
    PersonResponse update(Integer id, PersonUpdateRequest request);

    PersonResponse findById(Integer id);

    PersonResponse findByDocumentNumber(String documentNumber);

    /**
     * Listado paginado. Ojo al armar el Response: userId no cuelga de Person — la FK vive en usuario.
     * Resuélvelo con UserRepository.findByPersonIdIn sobre los ids de la página y arma un mapa, una
     * consulta por página; pedirlo persona por persona es un N+1.
     */
    Page<PersonResponse> findAll(Pageable pageable);

    /**
     * Búsqueda por nombre o documento (?buscar=). Sin filtro de eliminados: una persona eliminada ya
     * no es una fila.
     */
    Page<PersonResponse> search(String term, Pageable pageable);

    /**
     * activo = false vía JPA. El trigger trg_sincronizar_usuario_con_persona propaga al usuario.
     */
    void deactivate(Integer id);

    /**
     * Reactivación vía sp_reactivar_persona: queda auditada con el usuario del JWT, que el
     * procedimiento recibe por parámetro (patrón G).
     */
    PersonResponse reactivate(Integer id, Integer executedByUserId);

    /**
     * Eliminación definitiva vía sp_eliminar_persona. Irreversible: reasigna la huella de la persona
     * al registro sentinel y borra la fila. El procedimiento fija app.usuario_id por su cuenta.
     */
    void deletePermanently(Integer id, Integer executedByUserId);

    /**
     * Perfil consolidado desde vista_perfil_persona: datos, contacto de emergencia, saldo, días de
     * crédito y resumen de asistencias en una sola consulta.
     */
    PersonProfileViewResponse getProfile(Integer id);

    PersonProfileViewResponse getProfileByDocumentNumber(String documentNumber);

    /**
     * Saldo desde vista_saldo_persona. La vista solo incluye personas activas.
     */
    PersonBalanceViewResponse getBalance(Integer id);
}
