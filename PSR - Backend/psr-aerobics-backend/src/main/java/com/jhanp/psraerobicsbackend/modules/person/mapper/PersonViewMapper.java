package com.jhanp.psraerobicsbackend.modules.person.mapper;

import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonProfileViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.view.PersonBalanceView;
import com.jhanp.psraerobicsbackend.modules.person.view.PersonProfileView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Pasa las vistas de persona a sus DTO. Va aparte de PersonMapper, que trabaja sobre la tabla.
 * <p>
 * totalCredited (columna total_abonado) sale como totalPaid: es lo que la persona ya pagó, y así lo
 * consume el frontend.
 */
@Mapper(componentModel = "spring")
public interface PersonViewMapper {

    PersonProfileViewResponse toResponse(PersonProfileView view);

    @Mapping(source = "totalCredited", target = "totalPaid")
    PersonBalanceViewResponse toResponse(PersonBalanceView view);
}
