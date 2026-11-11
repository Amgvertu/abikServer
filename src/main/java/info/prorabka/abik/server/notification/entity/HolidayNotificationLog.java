package info.prorabka.abik.server.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Защита от повторной отправки праздничного напоминания или поздравления. */
@Entity
@Table(name = "holiday_notification_log", uniqueConstraints = @UniqueConstraint(
        name = "uq_holiday_notification",
        columnNames = {"recipient_user_id", "celebrated_user_id", "team_id",
                "holiday_type", "stage", "holiday_year"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "celebrated_user_id", nullable = false)
    private Long celebratedUserId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "holiday_type", nullable = false, length = 30)
    private String holidayType;

    @Column(nullable = false, length = 20)
    private String stage;

    @Column(name = "holiday_year", nullable = false)
    private Integer holidayYear;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    @PrePersist
    void onCreate() {
        if (sentAt == null) sentAt = OffsetDateTime.now();
    }
}
