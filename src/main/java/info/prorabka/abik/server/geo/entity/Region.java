package info.prorabka.abik.server.geo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Регион РФ (субъект).
 * Общий справочник — используется в профиле пользователя, командах и т.д.
 */
@Entity
@Table(name = "regions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** ISO 3166-2 код, например RU-MOW. */
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    /** IANA-идентификатор часового пояса, например "Europe/Moscow". */
    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}