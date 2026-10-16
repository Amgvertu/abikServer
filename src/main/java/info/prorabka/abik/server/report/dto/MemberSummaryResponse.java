package info.prorabka.abik.server.report.dto;

/**
 * Сводка по одному участнику команды.
 */
public record MemberSummaryResponse(
        Long userId,
        String lastName,
        String firstName,
        String phone,
        /** Всего начислено (в копейках). */
        Long totalChargesKopecks,
        /** Оплачено (в копейках). */
        Long totalPaidKopecks,
        /** Остаток долга (в копейках). */
        Long totalRemainingKopecks,
        /** Баланс (положительный — переплата, отрицательный — долг). */
        Long balanceKopecks
) {}