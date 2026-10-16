package info.prorabka.abik.server.payment.dto;

import java.time.OffsetDateTime;

/**
 * Баланс участника в команде.
 * Положительный — переплата, отрицательный — долг, ноль — всё закрыто.
 */
public record BalanceResponse(
        Long teamId,
        Long userId,
        String userLastName,
        String userFirstName,
        Long amountKopecks,
        OffsetDateTime updatedAt
) {}