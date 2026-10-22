package info.prorabka.abik.server.event.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_sweaters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(EventSweaterId.class)
public class EventSweater {

    @Id @Column(name = "event_id")
    private Long eventId;

    @Id @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SweaterColor color;
}