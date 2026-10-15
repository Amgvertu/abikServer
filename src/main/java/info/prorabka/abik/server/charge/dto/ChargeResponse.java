package info.prorabka.abik.server.charge.dto;

import info.prorabka.abik.server.charge.entity.Charge;
import info.prorabka.abik.server.charge.entity.ChargeStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Представление начисления.
 * Дополнительно подтягиваются: ФИО пользователя, название группы и подгруппы.
 */
public record ChargeResponse(
        Long id,
        Long teamId,
        Long templateId,
        Long groupId,
        String groupName,
        Long subgroupId,
        String subgroupName,
        Long userId,
        String userLastName,
        String userFirstName,
        Long amountKopecks,
        Long paidKopecks,
        Long remainingKopecks,
        ChargeStatus status,
        String description,
        LocalDate dueDate,
        Short periodYear,
        Short periodMonth,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime cancelledAt,
        String cancelReason
) {
    public static ChargeResponse from(Charge c,
                                      String groupName,
                                      String subgroupName,
                                      String lastName,
                                      String firstName) {
        return new ChargeResponse(
                c.getId(),
                c.getTeamId(),
                c.getTemplateId(),
                c.getGroupId(),
                groupName,
                c.getSubgroupId(),
                subgroupName,
                c.getUserId(),
                lastName,
                firstName,
                c.getAmountKopecks(),
                c.getPaidKopecks(),
                c.remainingKopecks(),
                c.getStatus(),
                c.getDescription(),
                c.getDueDate(),
                c.getPeriodYear(),
                c.getPeriodMonth(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getCancelledAt(),
                c.getCancelReason()
        );
    }
}