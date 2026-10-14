package info.prorabka.abik.server.team.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Запись о передаче владения командой.
 */
@Entity
@Table(name = "ownership_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnershipTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Column(name = "transferred_at", nullable = false)
    private OffsetDateTime transferredAt;

    @PrePersist
    void onCreate() {
        if (transferredAt == null) transferredAt = OffsetDateTime.now();
    }
}