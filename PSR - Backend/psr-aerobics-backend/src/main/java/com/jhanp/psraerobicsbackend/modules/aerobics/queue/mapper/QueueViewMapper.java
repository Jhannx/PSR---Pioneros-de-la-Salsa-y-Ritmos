package com.jhanp.psraerobicsbackend.modules.aerobics.queue.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.PrepaidQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.TodayQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.WaitingQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.view.PrepaidQueueView;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.view.TodayQueueView;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.view.WaitingQueueView;
import org.mapstruct.Mapper;

/**
 * Pasa las tres vistas de cola a sus DTO. Las tres siguen separadas hasta el final del recorrido:
 * la de la pantalla de TV no debe poder cargar saldo ni monto ni por accidente.
 */
@Mapper(componentModel = "spring")
public interface QueueViewMapper {

    TodayQueueViewResponse toResponse(TodayQueueView view);

    WaitingQueueViewResponse toResponse(WaitingQueueView view);

    PrepaidQueueViewResponse toResponse(PrepaidQueueView view);
}
