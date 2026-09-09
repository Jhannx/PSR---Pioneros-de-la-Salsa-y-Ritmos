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
     * Nunca se borra un método de pago: hay movimientos que lo referencian. Apagarlo es mandar
     * active = false en este mismo request, no un endpoint aparte.
     */
    PaymentMethodResponse update(Integer id, PaymentMethodRequest request);
}
