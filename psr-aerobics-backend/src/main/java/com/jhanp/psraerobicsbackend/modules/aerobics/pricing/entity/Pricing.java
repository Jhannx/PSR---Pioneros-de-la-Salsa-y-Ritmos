package com.jhanp.psraerobicsbackend.modules.aerobics.pricing.entity;

import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "tarifa")
public class Pricing {

    @Id
    private Integer id;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private User updatedBy;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = OffsetDateTime.now();
    }
}
