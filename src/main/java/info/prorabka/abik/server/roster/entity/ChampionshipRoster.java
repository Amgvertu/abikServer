package info.prorabka.abik.server.roster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "championship_roster")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChampionshipRoster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 20)
    private String season;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String status;      // ACTIVE, REMOVED

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @PrePersist
    void onCreate() {
        if (addedAt == null) addedAt = OffsetDateTime.now();
        if (status == null) status = "ACTIVE";
    }
}