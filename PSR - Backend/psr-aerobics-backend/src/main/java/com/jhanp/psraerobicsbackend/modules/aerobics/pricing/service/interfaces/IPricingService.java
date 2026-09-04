package com.jhanp.psraerobicsbackend.modules.aerobics.pricing.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.request.PricingRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.response.PricingResponse;

public interface IPricingService {

    /**
     * Tarifa vigente (fila única id = 1). Es la misma que cobra fn_tarifa_vigente() al confirmar
     * una asistencia.
     */
    PricingResponse getCurrent();

    /**
     * Cambio de tarifa por JPA. Exige fijar app.usuario_id en la misma transacción: el trigger
     * registra el cambio de monto en historial_cambio. actualizado_por lo fija la aplicación.
     */
    PricingResponse update(PricingRequest request, Integer updatedByUserId);
}
