package info.prorabka.abik.server.charge.entity;

/**
 * Статус начисления.
 */
public enum ChargeStatus {
    /** Активно, ничего не оплачено. */
    ACTIVE,
    /** Частично оплачено. */
    PARTIALLY_PAID,
    /** Полностью оплачено. */
    PAID,
    /** Отменено. */
    CANCELLED,
    /** В архиве (команда архивирована). */
    ARCHIVED
}