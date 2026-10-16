package info.prorabka.abik.server.payment.dto;

import java.time.LocalDate;

/**
 * Фильтр для списка платежей.
 * Все поля опциональны.
 */
public record PaymentFilterRequest(
        Long userId,
        LocalDate dateFrom,
        LocalDate dateTo,
        Long amountMinKopecks,
        Long amountMaxKopecks
) {}