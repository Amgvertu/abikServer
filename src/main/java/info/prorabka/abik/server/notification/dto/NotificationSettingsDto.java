package info.prorabka.abik.server.notification.dto;

import info.prorabka.abik.server.notification.entity.UserNotificationSettings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** GET-ответ и PUT-запрос персональных настроек напоминаний. */
public record NotificationSettingsDto(
        @NotNull Boolean remindersEnabled,
        @NotNull @Min(1) @Max(10) Integer reminderCount,
        @NotNull @Min(1) @Max(43200) Integer leadTimeMinutes,
        @NotNull @Min(1) @Max(43200) Integer intervalMinutes
) {
    public static NotificationSettingsDto from(UserNotificationSettings settings) {
        return new NotificationSettingsDto(
                settings.getRemindersEnabled(),
                settings.getReminderCount(),
                settings.getLeadTimeMinutes(),
                settings.getIntervalMinutes());
    }
}
