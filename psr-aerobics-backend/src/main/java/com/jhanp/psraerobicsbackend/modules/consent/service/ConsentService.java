package com.jhanp.psraerobicsbackend.modules.consent.service;

import com.jhanp.psraerobicsbackend.modules.consent.dto.response.ConsentResponse;
import com.jhanp.psraerobicsbackend.modules.consent.dto.response.ConsentStatusResponse;
import com.jhanp.psraerobicsbackend.modules.consent.entity.Consent;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentMedium;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentType;
import com.jhanp.psraerobicsbackend.modules.consent.mapper.ConsentMapper;
import com.jhanp.psraerobicsbackend.modules.consent.repository.ConsentRepository;
import com.jhanp.psraerobicsbackend.modules.consent.service.interfaces.IConsentService;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonRepository;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Escribe y lee el registro de consentimiento. No decide nada: quién necesita qué autorización lo
 * deciden los flujos que la exigen (alta, observaciones de salud, contacto de emergencia), porque es
 * ahí donde se sabe qué se está intentando hacer.
 */
@Service
@Slf4j
public class ConsentService implements IConsentService {

    private final ConsentRepository consentRepository;
    private final PersonRepository personRepository;
    private final ConsentMapper consentMapper;

    /**
     * La versión del documento que se está haciendo aceptar. Se congela en cada fila al insertarla.
     * <p>
     * En configuración y no en una tabla porque la decisión tomada es que la política se queda en v1
     * única, sin sistema de versiones; una tabla para una fila que cambia una vez al año —si acaso—
     * es maquinaria que hay que mantener a cambio de nada. El día que se publique una v2, se cambia
     * la propiedad y las filas nuevas quedan marcadas con ella solas.
     */
    private final String policyVersion;

    public ConsentService(ConsentRepository consentRepository,
                          PersonRepository personRepository,
                          ConsentMapper consentMapper,
                          @Value("${app.privacy.policy-version}") String policyVersion) {
        this.consentRepository = consentRepository;
        this.personRepository = personRepository;
        this.consentMapper = consentMapper;
        this.policyVersion = policyVersion;
    }

    /**
     * Sin propagación declarada en {@code @Transactional}, o sea REQUIRED: se une a la transacción de
     * quien llame. Es la pieza que hace atómico "alta + constancia", y cambiarlo a REQUIRES_NEW
     * rompería exactamente lo que esta tabla vino a arreglar — volvería posible que quedara la
     * constancia de un alta que luego se deshizo, o al revés.
     */
    @Override
    @Transactional
    public ConsentResponse record(Integer personId,
                                  ConsentType type,
                                  boolean granted,
                                  ConsentMedium medium,
                                  Integer registeredByUserId) {

        // La FK ya lo impediría, pero como un error de base de datos: este endpoint es público por
        // /api/personas/{id}/consentimientos y un id inventado debe responder 404, no 500.
        if (!personRepository.existsById(personId)) {
            throw new ResourceNotFoundException("No existe la persona " + personId);
        }

        Consent consent = Consent.builder()
                .personId(personId)
                .type(type)
                .granted(granted)
                .policyVersion(policyVersion)
                .medium(medium)
                .registeredByUserId(registeredByUserId)
                .build();

        Consent saved = consentRepository.save(consent);

        // Revocar el consentimiento de salud tiene que llevarse el dato por delante.
        //
        // Si la fila dijera "revocado" y observaciones_salud siguiera llena, la revocación no
        // significaría nada: el sistema seguiría tratando un dato sensible sin permiso, y encima
        // habría dejado constancia escrita de que sabía que no lo tenía. Se hace en la misma
        // transacción para que no exista ningún instante en que una cosa vaya sin la otra.
        //
        // Los otros tres tipos no tienen un campo propio del que tirar: el general y el de
        // participación voluntaria afectan a todo el registro —revocarlos es una solicitud de
        // supresión, que se atiende por el canal del numeral 10, no borrando la ficha desde aquí— y
        // el de terceros lo ejerce el propio contacto pidiendo que lo quiten.
        if (type == ConsentType.HEALTH_DATA && !granted) {
            personRepository.findById(personId).ifPresent(person -> {
                person.setHealthObservations(null);
                personRepository.save(person);
            });
        }

        // Se registra el hecho, no el contenido: el log dice qué tipo y de quién, y nada más.
        log.info("Consentimiento {} {} para la persona {} (medio {})",
                type, granted ? "otorgado" : "revocado", personId, medium);

        return consentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActive(Integer personId, ConsentType type) {
        return consentRepository.findFirstByPersonIdAndTypeOrderByRecordedAtDescIdDesc(personId, type)
                .map(Consent::getGranted)
                .orElse(false);
    }

    /**
     * Una sola consulta para el estado y el historial.
     * <p>
     * Podría resolverse con cuatro {@code findFirst...} más uno para el historial, pero serían cinco
     * viajes a la base para leer una tabla que, por persona, tiene un puñado de filas. El historial ya
     * viene ordenado de más reciente a más antiguo, así que la primera aparición de cada tipo es
     * justamente su estado vigente.
     */
    @Override
    @Transactional(readOnly = true)
    public ConsentStatusResponse getStatus(Integer personId) {
        if (!personRepository.existsById(personId)) {
            throw new ResourceNotFoundException("No existe la persona " + personId);
        }

        List<Consent> history = consentRepository.findByPersonIdOrderByRecordedAtDescIdDesc(personId);

        Map<ConsentType, Consent> current = new EnumMap<>(ConsentType.class);
        history.forEach(consent -> current.putIfAbsent(consent.getType(), consent));

        return new ConsentStatusResponse(
                personId,
                policyVersion,
                isGranted(current, ConsentType.DATA_PROCESSING),
                isGranted(current, ConsentType.VOLUNTARY_PARTICIPATION),
                isGranted(current, ConsentType.HEALTH_DATA),
                isGranted(current, ConsentType.THIRD_PARTY_DATA),
                !current.containsKey(ConsentType.DATA_PROCESSING),
                history.stream().map(consentMapper::toResponse).toList());
    }

    private boolean isGranted(Map<ConsentType, Consent> current, ConsentType type) {
        Consent consent = current.get(type);
        return consent != null && Boolean.TRUE.equals(consent.getGranted());
    }
}
