package info.prorabka.abik.server.event.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "event_series")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType type;

    @Column(name = "time_of_day", nullable = false, length = 5)
    private String timeOfDay;       // "21:30"

    @Column(nullable = false, length = 500)
    private String place;

    @Column(length = 1000)
    private String note;

    /** Дни недели 1..7 (пн..вс). Храним как массив — PostgreSQL SMALLINT[]. */
    @Column(name = "weekdays", nullable = false, columnDefinition = "smallint[]")
    private Short[] weekdays;

    @Column(name = "weeks_ahead", nullable = false)
    private Integer weeksAhead;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}