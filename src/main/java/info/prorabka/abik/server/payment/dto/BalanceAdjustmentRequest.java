package info.prorabka.abik.server.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BalanceAdjustmentRequest(

        @NotNull(message = "Сумма обязательна")
        Long amountKopecks,

        @NotBlank(message = "Укажите причину корректировки")
        @Size(max = 500)
        String reason
) {}