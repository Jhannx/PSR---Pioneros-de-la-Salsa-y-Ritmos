package com.jhanp.psraerobicsbackend.modules.aerobics.pricing.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.entity.Pricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Tabla de fila única: la tarifa vigente siempre es el id 1 (chk_tarifa_fila_unica).
 * El UPDATE lo audita trg_tarifa_actualizar_y_auditar, que exige app.usuario_id en la transacción.
 */
@Repository
public interface PricingRepository extends JpaRepository<Pricing, Integer> {

    Integer CURRENT_PRICING_ID = 1;
}
