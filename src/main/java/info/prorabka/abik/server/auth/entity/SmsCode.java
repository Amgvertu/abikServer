package info.prorabka.abik.server.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * SMS-код подтверждения.
 * Соответствует таблице sms_codes.
 */
@Entity
@Table(name = "sms_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 10)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SmsPurpose purpose;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private Boolean used;

    @Column(nullable = false)
    private Short attempts;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (used == null) used = false;
        if (attempts == null) attempts = 0;
    }
}