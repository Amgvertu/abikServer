package info.prorabka.abik.server.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Запрос на создание команды.
 */
public record TeamCreateRequest(

        @NotBlank(message = "Название команды обязательно")
        @Size(max = 150)
        String name,

        @NotNull(message = "Город обязателен")
        Long cityId,

        @NotNull(message = "Домашний регион обязателен")
        Long homeRegionId,

        @NotBlank(message = "Логотип обязателен")
        @Size(max = 500)
        String logoUrl,

        @Size(max = 5000)
        String description,

        @Size(max = 50)
        String season
) {}