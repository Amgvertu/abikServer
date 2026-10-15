package info.prorabka.abik.server.charge.dto;

import info.prorabka.abik.server.charge.entity.ChargeSubgroup;

import java.time.OffsetDateTime;

public record ChargeSubgroupResponse(
        Long id,
        Long groupId,
        String name,
        OffsetDateTime createdAt
) {
    public static ChargeSubgroupResponse from(ChargeSubgroup s) {
        return new ChargeSubgroupResponse(s.getId(), s.getGroupId(), s.getName(), s.getCreatedAt());
    }
}