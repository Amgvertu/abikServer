package info.prorabka.abik.server.charge.dto;

import info.prorabka.abik.server.charge.entity.ChargeDistribution;
import info.prorabka.abik.server.charge.entity.ChargeTemplate;
import info.prorabka.abik.server.charge.entity.ChargeType;
import info.prorabka.abik.server.charge.entity.Periodicity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Представление шаблона начисления.
 */
public record ChargeTemplateResponse(
        Long id,
        Long teamId,
        Long groupId,
        Long subgroupId,
        String name,
        Long amountKopecks,
        ChargeType type,
        Periodicity periodicity,
        Short dayOfMonth,
        LocalDate startDate,
        LocalDate endDate,
        ChargeDistribution distribution,
        Boolean active,
        Set<Long> memberIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ChargeTemplateResponse from(ChargeTemplate t) {
        return new ChargeTemplateResponse(
                t.getId(),
                t.getTeamId(),
                t.getGroupId(),
                t.getSubgroupId(),
                t.getName(),
                t.getAmountKopecks(),
                t.getType(),
                t.getPeriodicity(),
                t.getDayOfMonth(),
                t.getStartDate(),
                t.getEndDate(),
                t.getDistribution(),
                t.getActive(),
                t.getMemberIds(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}