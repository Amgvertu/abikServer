package info.prorabka.abik.server.charge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Ежемесячный опрос об абонементе на следующий календарный месяц. */
@Entity
@Table(name = "subscription_polls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPoll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "amount_kopecks", nullable = false)
    private Long amountKopecks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeDistribution distribution;

    /** Месяц, в котором проводится опрос. */
    @Column(name = "period_year", nullable = false)
    private Short periodYear;

    @Column(name = "period_month", nullable = false)
    private Short periodMonth;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

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
