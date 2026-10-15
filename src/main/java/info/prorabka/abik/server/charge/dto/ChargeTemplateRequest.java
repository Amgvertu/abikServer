package info.prorabka.abik.server.charge.dto;

import info.prorabka.abik.server.charge.entity.ChargeDistribution;
import info.prorabka.abik.server.charge.entity.ChargeType;
import info.prorabka.abik.server.charge.entity.Periodicity;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Set;

/**
 * Создание или обновление шаблона начисления.
 */
public record ChargeTemplateRequest(

        @NotNull(message = "Группа обязательна")
        Long groupId,

        Long subgroupId,

        @NotBlank(message = "Название шаблона обязательно")
        @Size(max = 200)
        String name,

        @NotNull(message = "Сумма обязательна")
        @Min(value = 1, message = "Сумма должна быть больше нуля (в копейках)")
        Long amountKopecks,

        @NotNull(message = "Тип обязателен")
        ChargeType type,

        @NotNull(message = "Периодичность обязательна")
        Periodicity periodicity,

        /** Только для SYSTEMATIC. 1-28. */
        @Min(value = 1, message = "День месяца от 1 до 28")
        @Max(value = 28, message = "День месяца от 1 до 28")
        Short dayOfMonth,

        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "Способ распределения обязателен")
        ChargeDistribution distribution,

        @NotEmpty(message = "Укажите хотя бы одного участника")
        Set<Long> memberIds,

        Boolean active
) {}