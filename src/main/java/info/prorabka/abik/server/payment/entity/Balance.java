package info.prorabka.abik.server.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Кеш баланса участника в команде.
 * Композитный PK: (team_id, user_id).
 */
@Entity
@Table(name = "balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Balance {

    @EmbeddedId
    private BalanceId id;

    @Column(name = "amount_kopecks", nullable = false)
    private Long amountKopecks;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}