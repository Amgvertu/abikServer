package info.prorabka.abik.server.user.entity;

/**
 * Статусы пользователя.
 * Значения должны совпадать с CHECK-констрейнтом в БД (V2__auth_tables.sql).
 */
public enum UserStatus {
    /** Зарегистрирован, но SMS ещё не подтверждён. */
    PENDING,
    /** Активен, может логиниться. */
    ACTIVE,
    /** Заблокирован суперадмином. */
    BLOCKED,
    /** Удалён (аккаунт сохраняется для истории). */
    DELETED
}