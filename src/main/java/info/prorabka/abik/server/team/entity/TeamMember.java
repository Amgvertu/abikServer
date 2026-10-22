package info.prorabka.abik.server.team.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Участник команды.
 */
@Entity
@Table(name = "team_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "jersey_number")
    private Short jerseyNumber;

    @Column(name = "is_trainer", nullable = false)
    private Boolean isTrainer = false;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "excluded_at")
    private OffsetDateTime excludedAt;

    @PrePersist
    void onCreate() {
        if (joinedAt == null) joinedAt = OffsetDateTime.now();
        if (status == null) status = MemberStatus.ACTIVE;
    }
}