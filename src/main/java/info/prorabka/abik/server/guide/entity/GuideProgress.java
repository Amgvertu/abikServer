package info.prorabka.abik.server.guide.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "guide_progress", uniqueConstraints = @UniqueConstraint(
        name = "uq_guide_progress", columnNames = {"user_id", "screen_key", "role_key"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuideProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "screen_key", nullable = false, length = 80) private String screenKey;
    @Column(name = "role_key", nullable = false, length = 40) private String roleKey;
    @Column(name = "completed_at", nullable = false) private OffsetDateTime completedAt;
    @PrePersist void onCreate() { if (completedAt == null) completedAt = OffsetDateTime.now(); }
}
