package info.prorabka.abik.server.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Расширенный профиль пользователя.
 * Связан с User один-к-одному через общий PK (user_id).
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    /** Общий PK с User. */
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", nullable = false, length = 100)
    private String middleName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "height_cm", nullable = false)
    private Short heightCm;

    @Column(name = "weight_kg", nullable = false)
    private Short weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "stick_hand", nullable = false, length = 10)
    private StickHand stickHand;

    @Column(name = "jersey_size", length = 20)
    private String jerseySize;

    @Column(name = "covers_size", length = 20)
    private String coversSize;

    @Column(name = "gaiters_size", length = 20)
    private String gaitersSize;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Амплуа — множество, хранится в user_positions.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_positions",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "position", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<UserPosition> positions = new HashSet<>();

    @PrePersist
    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}