package info.prorabka.abik.server.charge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Создание или обновление группы начислений.
 */
public record ChargeGroupRequest(

        @NotBlank(message = "Название группы обязательно")
        @Size(max = 150)
        String name
) {}