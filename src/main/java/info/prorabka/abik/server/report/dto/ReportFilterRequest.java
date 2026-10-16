package info.prorabka.abik.server.report.dto;

import info.prorabka.abik.server.charge.entity.ChargeStatus;

import java.time.LocalDate;

/**
 * Фильтр для отчётов. Все поля опциональны.
 */
public record ReportFilterRequest(
        Long userId,
        Long groupId,
        ChargeStatus status,
        LocalDate dateFrom,
        LocalDate dateTo,
        Long amountMinKopecks,
        Long amountMaxKopecks
) {}