package info.prorabka.abik.server.event.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_lineups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventLineup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LineupRole role;

    @Column(name = "line_number")
    private Short lineNumber;
}