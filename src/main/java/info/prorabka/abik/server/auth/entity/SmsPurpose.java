package info.prorabka.abik.server.auth.entity;

/**
 * Назначение SMS-кода.
 * Значения должны совпадать с CHECK-констрейнтом в БД (V2__auth_tables.sql).
 */
public enum SmsPurpose {
    /** Подтверждение регистрации. */
    REGISTRATION,
    /** Восстановление пароля. */
    PASSWORD_RESET,
    /** Подтверждение старого номера при смене телефона. */
    PHONE_CHANGE_OLD,
    /** Подтверждение нового номера при смене телефона. */
    PHONE_CHANGE_NEW
}