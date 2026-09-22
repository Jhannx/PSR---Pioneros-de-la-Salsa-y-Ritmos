package com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.entity.QueueConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Tabla de fila única: la configuración vigente siempre es el id 1 (chk_configuracion_cola_fila_unica).
 * El UPDATE lo audita trg_configuracion_cola_actualizar_y_auditar, que exige app.usuario_id.
 */
@Repository
public interface QueueConfigRepository extends JpaRepository<QueueConfig, Integer> {

    Integer CURRENT_QUEUE_CONFIG_ID = 1;
}
