package info.prorabka.abik.server.charge.entity;

/**
 * Способ распределения суммы.
 */
public enum ChargeDistribution {
    /** Сумма делится поровну между участниками. */
    EQUAL,
    /** Фиксированная сумма с каждого участника. */
    FIXED
}