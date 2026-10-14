package info.prorabka.abik.server.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Пользователь системы.
 * Соответствует таблице users.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Телефон в формате E.164, уникален. */
    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    /** Хеш пароля (Argon2id). Никогда не хранится в открытом виде. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Статус аккаунта. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /** Заполняем createdAt/updatedAt автоматически перед сохранением. */
    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
