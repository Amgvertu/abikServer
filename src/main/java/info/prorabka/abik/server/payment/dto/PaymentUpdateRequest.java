package info.prorabka.abik.server.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PaymentUpdateRequest(

        @NotNull(message = "Сумма обязательна")
        @Min(value = 1, message = "Сумма должна быть больше нуля (в копейках)")
        Long amountKopecks,

        @NotNull(message = "Пользователь обязателен")
        Long userId,               // ← НОВОЕ (теперь можно менять)

        LocalDate paidAt,          // ← НОВОЕ

        @Size(max = 500)
        String description
) {}