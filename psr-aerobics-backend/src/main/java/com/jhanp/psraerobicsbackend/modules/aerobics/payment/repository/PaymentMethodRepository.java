package com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {

    List<PaymentMethod> findByActiveTrue();

    Optional<PaymentMethod> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
