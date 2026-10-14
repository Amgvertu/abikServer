package info.prorabka.abik.server.charge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Шаблон начисления — правило, по которому генерируются charges.
 */
@Entity
@Table(name = "charge_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "subgroup_id")
    private Long subgroupId;

    @Column(nullable = false, length = 200)
    private String name;

    /** Сумма в копейках. */
    @Column(name = "amount_kopecks", nullable = false)
    private Long amountKopecks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Periodicity periodicity;

    /** День месяца 1-28, только для SYSTEMATIC. */
    @Column(name = "day_of_month")
    private Short dayOfMonth;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeDistribution distribution;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Участники шаблона (к кому применяется).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "charge_template_members",
            joinColumns = @JoinColumn(name = "template_id")
    )
    @Column(name = "user_id", nullable = false)
    @Builder.Default
    private Set<Long> memberIds = new HashSet<>();

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (active == null) active = true;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}