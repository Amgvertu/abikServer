package info.prorabka.abik.server.offline.dto;

import java.time.LocalDate;
import java.util.Set;

/** Поля названия позволяют создать ещё не существовавшие при вводе офлайн группу и подгруппу. */
public record OfflineChargeCreateRequest(
        Long groupId,
        String groupName,
        Long subgroupId,
        String subgroupName,
        Set<Long> userIds,
        Long amountKopecks,
        String description,
        LocalDate dueDate
) {}
