package info.prorabka.abik.server.charge.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Set;

/**
 * Ручное создание разового начисления.
 * Используется, когда начисление не сгенерировано из шаблона.
 */
public record ChargeCreateManualRequest(

        @NotNull(message = "Группа обязательна")
        Long groupId,

        Long subgroupId,

        @NotEmpty(message = "Укажите хотя бы одного участника")
        Set<Long> userIds,

        @NotNull(message = "Сумма обязательна")
        @Min(value = 1, message = "Сумма должна быть больше нуля (в копейках)")
        Long amountKopecks,

        @Size(max = 500)
        String description,

        @NotNull(message = "Дата начисления обязательна")
        LocalDate dueDate
) {}