package info.prorabka.abik.server.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Согласие пользователя на обработку персональных данных (152-ФЗ).
 * Соответствует таблице user_consents.
 */
@Entity
@Table(name = "user_consents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "policy_version", nullable = false, length = 50)
    private String policyVersion;

    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @PrePersist
    void onCreate() {
        if (acceptedAt == null) acceptedAt = OffsetDateTime.now();
    }
}