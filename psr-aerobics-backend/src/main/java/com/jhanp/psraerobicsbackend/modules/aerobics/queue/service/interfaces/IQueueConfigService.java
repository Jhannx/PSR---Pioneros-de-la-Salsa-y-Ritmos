package com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.request.QueueConfigRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.QueueConfigResponse;

public interface IQueueConfigService {

    /**
     * Configuración vigente (fila única id = 1): personas por turno, margen de ingreso y minutos de
     * visibilidad de pagos. El margen lo usa fn_horario_vigente_ahora() para resolver la sesión.
     */
    QueueConfigResponse getCurrent();

    /**
     * Actualización por JPA. Exige fijar app.usuario_id en la misma transacción: el trigger audita
     * cada uno de los tres campos por separado.
     */
    QueueConfigResponse update(QueueConfigRequest request, Integer updatedByUserId);
}
