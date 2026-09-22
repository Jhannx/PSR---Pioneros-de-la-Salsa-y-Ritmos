package com.jhanp.psraerobicsbackend.modules.aerobics.pricing.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.request.PricingRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.response.PricingResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.entity.Pricing;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.mapper.PricingMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.repository.PricingRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.service.interfaces.IPricingService;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.persistence.AuditContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PricingService implements IPricingService {

    private final PricingRepository pricingRepository;
    private final PricingMapper pricingMapper;
    private final AuditContextRepository auditContextRepository;
    private final UserRepository userRepository;

    @Override
    public PricingResponse getCurrent() {
        return pricingMapper.toResponse(findCurrent());
    }

    @Override
    @Transactional
    public PricingResponse update(PricingRequest request, Integer updatedByUserId) {
        // El usuario viene del JWT, así que existe: basta la referencia para la FK, sin consultarlo.
        // Patrón C: trg_tarifa_actualizar_y_auditar lee app.usuario_id en esta misma transacción.
        auditContextRepository.setCurrentUser(updatedByUserId);

        Pricing pricing = findCurrent();
        pricing.setAmount(request.amount());
        pricing.setUpdatedBy(userRepository.getReferenceById(updatedByUserId));

        log.info("Tarifa actualizada a {} por el usuario ID: {}", request.amount(), updatedByUserId);
        return pricingMapper.toResponse(pricingRepository.save(pricing));
    }

    private Pricing findCurrent() {
        return pricingRepository.findById(PricingRepository.CURRENT_PRICING_ID)
                .orElseThrow(() -> new ResourceNotFoundException("No hay tarifa configurada en el sistema"));
    }
}
