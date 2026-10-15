package info.prorabka.abik.server.charge.dto;

import info.prorabka.abik.server.charge.entity.ChargeStatus;

import java.time.LocalDate;

/**
 * Параметры фильтрации списка начислений.
 * Все поля опциональны — можно комбинировать.
 */
public record ChargeFilterRequest(
        Long userId,
        Long groupId,
        Long subgroupId,
        ChargeStatus status,
        LocalDate dateFrom,
        LocalDate dateTo,
        Long amountMinKopecks,
        Long amountMaxKopecks
) {}