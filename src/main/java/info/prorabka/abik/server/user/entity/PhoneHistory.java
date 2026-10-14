package info.prorabka.abik.server.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * История смены телефонных номеров.
 * Соответствует таблице phone_history.
 */
@Entity
@Table(name = "phone_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Старый номер. NULL для самой первой записи (первичная регистрация). */
    @Column(name = "old_phone", length = 20)
    private String oldPhone;

    @Column(name = "new_phone", nullable = false, length = 20)
    private String newPhone;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @PrePersist
    void onCreate() {
        if (changedAt == null) changedAt = OffsetDateTime.now();
    }
}