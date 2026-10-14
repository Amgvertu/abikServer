package info.prorabka.abik.server.team.entity;

/**
 * Статус приглашения в команду.
 */
public enum InvitationStatus {
    /** Ожидает ответа пользователя. */
    PENDING,
    /** Пользователь принял. */
    ACCEPTED,
    /** Пользователь отклонил. */
    DECLINED,
    /** Отменено отправителем. */
    CANCELLED,
    /** Истекло (7 дней без ответа). */
    EXPIRED
}