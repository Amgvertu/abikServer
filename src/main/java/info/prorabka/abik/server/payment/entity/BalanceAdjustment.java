package info.prorabka.abik.server.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Ручная корректировка баланса участника.
 */
@Entity
@Table(name = "balance_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Сумма в копейках: + переплата, - долг. */
    @Column(name = "amount_kopecks", nullable = false)
    private Long amountKopecks;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}