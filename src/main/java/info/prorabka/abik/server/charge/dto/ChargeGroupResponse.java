package info.prorabka.abik.server.charge.dto;

import info.prorabka.abik.server.charge.entity.ChargeGroup;

import java.time.OffsetDateTime;

/**
 * Представление группы начислений.
 */
public record ChargeGroupResponse(
        Long id,
        Long teamId,
        String name,
        OffsetDateTime createdAt
) {
    public static ChargeGroupResponse from(ChargeGroup g) {
        return new ChargeGroupResponse(g.getId(), g.getTeamId(), g.getName(), g.getCreatedAt());
    }
}