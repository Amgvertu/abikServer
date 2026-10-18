package info.prorabka.abik.server.payment.dto;

import info.prorabka.abik.server.payment.entity.BalanceAdjustment;

import java.time.OffsetDateTime;

public record BalanceAdjustmentResponse(
        Long id,
        Long teamId,
        Long userId,
        Long amountKopecks,
        String reason,
        Long createdByUserId,
        OffsetDateTime createdAt
) {
    public static BalanceAdjustmentResponse from(BalanceAdjustment b) {
        return new BalanceAdjustmentResponse(
                b.getId(),
                b.getTeamId(),
                b.getUserId(),
                b.getAmountKopecks(),
                b.getReason(),
                b.getCreatedByUserId(),
                b.getCreatedAt()
        );
    }
}