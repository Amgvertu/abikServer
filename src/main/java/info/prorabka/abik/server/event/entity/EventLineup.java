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

    /** Номер звена (null для вратаря). */
    @Column(name = "line_number")
    private Short lineNumber;

    /** Номер слота внутри позиции: вратарь — 1; защитники — 1..2; нападающие — 1..3. */
    @Column(name = "slot_number", nullable = false)
    private Short slotNumber;
}