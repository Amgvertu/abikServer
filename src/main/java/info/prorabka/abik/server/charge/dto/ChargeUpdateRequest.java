package info.prorabka.abik.server.charge.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Редактирование начисления (ТЗ 7.1).
 * Меняются: группа/подгруппа, сумма, комментарий, дата начисления.
 * Участник начисления не меняется — для переноса на другого участника
 * начисление отменяют и создают новое.
 */
public record ChargeUpdateRequest(

        @NotNull(message = "Группа обязательна")
        Long groupId,

        Long subgroupId,

        @NotNull(message = "Сумма обязательна")
        @Min(value = 1, message = "Сумма должна быть больше нуля (в копейках)")
        Long amountKopecks,

        @Size(max = 500)
        String description,

        @NotNull(message = "Дата начисления обязательна")
        LocalDate dueDate
) {}