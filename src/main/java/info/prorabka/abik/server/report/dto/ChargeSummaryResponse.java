package info.prorabka.abik.server.report.dto;

/**
 * Сводка по начислениям команды.
 */
public record ChargeSummaryResponse(
        Long teamId,
        /** Общая сумма всех активных начислений (в копейках). */
        Long totalChargesKopecks,
        /** Оплачено (в копейках). */
        Long totalPaidKopecks,
        /** Остаток к оплате = total − paid (в копейках). */
        Long totalRemainingKopecks,
        /** Общий баланс команды: сумма переплат минус сумма долгов (в копейках). */
        Long totalBalanceKopecks,
        /** Число активных начислений. */
        long activeChargesCount,
        /** Число полностью оплаченных. */
        long paidChargesCount,
        /** Число частично оплаченных. */
        long partiallyPaidChargesCount,
        /** Число отменённых. */
        long cancelledChargesCount
) {}