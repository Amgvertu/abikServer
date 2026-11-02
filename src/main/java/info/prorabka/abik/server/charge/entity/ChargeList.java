package info.prorabka.abik.server.charge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Именованный список получателей начислений: название + участники команды.
 * Используется как быстрый выбор получателей в формах начислений и шаблонов.
 */
@Entity
@Table(name = "charge_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 200)
    private String name;

    /** Участники списка. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "charge_list_members",
            joinColumns = @JoinColumn(name = "list_id")
    )
    @Column(name = "user_id", nullable = false)
    @Builder.Default
    private Set<Long> memberIds = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
