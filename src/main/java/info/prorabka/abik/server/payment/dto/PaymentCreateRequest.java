package info.prorabka.abik.server.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Запрос на внесение платежа.
 * Клиент указывает только «за кого» и «сколько» — распределение сервер делает сам.
 */
public record PaymentCreateRequest(

        @NotNull(message = "Пользователь обязателен")
        Long userId,

        @NotNull(message = "Сумма обязательна")
        @Min(value = 1, message = "Сумма должна быть больше нуля (в копейках)")
        Long amountKopecks,

        LocalDate paidAt,

        @Size(max = 500)
        String description
) {}