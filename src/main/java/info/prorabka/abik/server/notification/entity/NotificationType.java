package info.prorabka.abik.server.notification.entity;

/**
 * Типы уведомлений по ТЗ (стр. 7).
 */
public enum NotificationType {
    /** Приглашение в команду. */
    INVITATION,
    /** Смена роли участника. */
    ROLE_CHANGED,
    /** Новое начисление. */
    NEW_CHARGE,
    /** Внесён платёж. */
    PAYMENT,
    /** Напоминание о долге. */
    DEBT_REMINDER,
    /** Передача владения командой. */
    OWNERSHIP_TRANSFER,
    /** Участник исключён из команды. */
    EXCLUDED,
    /** Команда архивирована. */
    ARCHIVED,
    EVENT_CREATED,
    EVENT_UPDATED,
    EVENT_CANCELLED,
    ROSTER_ADDED,
    ROSTER_REMOVED,
    TRAINER_ASSIGNED,
    /** Напоминание о тренировке накануне (за день до события). */
    EVENT_REMINDER,
    /** Напоминание команде о завтрашнем празднике. */
    HOLIDAY_REMINDER,
    /** Персональное поздравление в день праздника. */
    HOLIDAY_GREETING
}