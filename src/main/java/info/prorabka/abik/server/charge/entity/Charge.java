package info.prorabka.abik.server.charge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Начисление — конкретный долг пользователя перед командой.
 */
@Entity
@Table(name = "charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /** От какого шаблона (может быть null для ручных разовых). */
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "subgroup_id")
    private Long subgroupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Сумма долга в копейках. */
    @Column(name = "amount_kopecks", nullable = false)
    private Long amountKopecks;

    /** Оплачено в копейках. */
    @Column(name = "paid_kopecks", nullable = false)
    private Long paidKopecks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeStatus status;

    @Column(length = 500)
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "period_year")
    private Short periodYear;

    @Column(name = "period_month")
    private Short periodMonth;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (paidKopecks == null) paidKopecks = 0L;
        if (status == null) status = ChargeStatus.ACTIVE;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /** Остаток к оплате в копейках. */
    public long remainingKopecks() {
        return Math.max(0, amountKopecks - paidKopecks);
    }
}