package info.prorabka.abik.server.event.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "series_id")
    private Long seriesId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventSection section;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType type;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "time_of_day", nullable = false, length = 5)
    private String timeOfDay;

    @Column(nullable = false, length = 500)
    private String place;

    @Column(length = 1000)
    private String note;

    @Column(name = "required_players")
    private Integer requiredPlayers;

    /** NONE, MANUAL, TEMPLATE */
    @Column(name = "charge_mode", nullable = false, length = 20)
    private String chargeMode;

    @Column(name = "charge_amount_kopecks")
    private Long chargeAmountKopecks;

    /** EACH_ROSTER, EACH_ATTENDED, SPLIT_ATTENDED, SPLIT_ROSTER */
    @Column(name = "charge_distribution", length = 20)
    private String chargeDistribution;

    @Column(name = "charge_template_id")
    private Long chargeTemplateId;

    /**
     * Явный список получателей ручного начисления (режим MANUAL).
     * Пустой список — получатели определяются по способу начисления
     * (отметившиеся/заявленные).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "event_charge_members",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @Column(name = "user_id", nullable = false)
    @Builder.Default
    private java.util.Set<Long> chargeMemberIds = new java.util.HashSet<>();

    @Column(name = "charged_at")
    private OffsetDateTime chargedAt;

    @Column(nullable = false, length = 20)
    private String status;      // ACTIVE, CANCELLED

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = "ACTIVE";
        if (chargeMode == null) chargeMode = "NONE";
    }

    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(); }
}