package info.prorabka.abik.server.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Распределение платежа на конкретное начисление.
 * Платёж 1000 ₽ может «размазаться» на несколько charges несколькими строками.
 */
@Entity
@Table(name = "payment_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "charge_id", nullable = false)
    private Long chargeId;

    @Column(name = "amount_kopecks", nullable = false)
    private Long amountKopecks;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}