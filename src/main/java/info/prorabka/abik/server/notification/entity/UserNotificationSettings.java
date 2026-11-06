package info.prorabka.abik.server.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Персональные настройки напоминаний; обязательные уведомления здесь не отключаются. */
@Entity
@Table(name = "user_notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "reminders_enabled", nullable = false)
    @Builder.Default
    private Boolean remindersEnabled = true;

    @Column(name = "reminder_count", nullable = false)
    @Builder.Default
    private Integer reminderCount = 1;

    @Column(name = "lead_time_minutes", nullable = false)
    @Builder.Default
    private Integer leadTimeMinutes = 1440;

    @Column(name = "interval_minutes", nullable = false)
    @Builder.Default
    private Integer intervalMinutes = 60;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static UserNotificationSettings defaults(Long userId) {
        return UserNotificationSettings.builder().userId(userId).build();
    }

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = OffsetDateTime.now();
        if (remindersEnabled == null) remindersEnabled = true;
        if (reminderCount == null) reminderCount = 1;
        if (leadTimeMinutes == null) leadTimeMinutes = 1440;
        if (intervalMinutes == null) intervalMinutes = 60;
    }
}
