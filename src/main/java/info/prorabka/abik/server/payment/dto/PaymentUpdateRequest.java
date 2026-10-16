package info.prorabka.abik.server.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Запрос на редактирование платежа.
 * Меняем только сумму и описание; пользователь (за кого) — не меняется.
 */
public record PaymentUpdateRequest(

        @NotNull(message = "Сумма обязательна")
        @Min(value = 1, message = "Сумма должна быть больше нуля (в копейках)")
        Long amountKopecks,

        @Size(max = 500)
        String description
) {}