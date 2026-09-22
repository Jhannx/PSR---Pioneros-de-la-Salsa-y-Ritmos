package com.jhanp.psraerobicsbackend.modules.consent.repository;

import com.jhanp.psraerobicsbackend.modules.consent.entity.Consent;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, Long> {

    /**
     * El estado vigente de un tipo para una persona: su fila más reciente, otorgada o revocada.
     * <p>
     * <b>El desempate por id no es adorno.</b> Ordenar solo por fecha deja el resultado indefinido
     * cuando dos filas comparten instante, y eso pasa de verdad: otorgar y revocar el consentimiento
     * de salud en la misma transacción (guardar una observación y borrarla acto seguido) puede
     * producir dos TIMESTAMPTZ idénticos. Sin el id, la consulta podría devolver la revocación o la
     * autorización según le apeteciera al planificador.
     */
    Optional<Consent> findFirstByPersonIdAndTypeOrderByRecordedAtDescIdDesc(Integer personId, ConsentType type);

    /** El historial completo, del más reciente al más antiguo. Es lo que se enseña si alguien pregunta. */
    List<Consent> findByPersonIdOrderByRecordedAtDescIdDesc(Integer personId);
}
