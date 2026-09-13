package com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.PaymentMethodRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PaymentMethodResponse;

import java.util.List;

public interface IPaymentMethodService {

    List<PaymentMethodResponse> findAll();

    /**
     * Métodos activos, para poblar el selector de cobro.
     */
    List<PaymentMethodResponse> findAllActive();

    PaymentMethodResponse findById(Integer id);

    PaymentMethodResponse create(PaymentMethodRequest request);

    /**
     * Nunca se borra un método de pago: hay movimientos que lo referencian. Para apagarlo está
     * {@link #setActive}; este PUT sirve para renombrarlo, y de paso sigue aceptando active.
     */
    PaymentMethodResponse update(Integer id, PaymentMethodRequest request);

    /**
     * Enciende o apaga el método. Existe aparte del PUT por dos razones, y la segunda importa:
     * <ol>
     *   <li>Es el patrón que ya usan usuarios y horarios
     *       ({@code PATCH /{id}/activar} y {@code /desactivar}), así que la API se lee igual en los
     *       tres sitios.</li>
     *   <li>El PUT exige {@code name} —es @NotBlank—, así que apagar un método por ahí obliga al
     *       cliente a reenviar el nombre que tuviera cargado en pantalla. Si alguien lo renombró
     *       mientras tanto, ese cambio se pisa. Un PATCH sin cuerpo no puede pisar nada.</li>
     * </ol>
     */
    PaymentMethodResponse setActive(Integer id, boolean active);
}
