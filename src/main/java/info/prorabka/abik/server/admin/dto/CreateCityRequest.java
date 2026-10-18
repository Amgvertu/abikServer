package info.prorabka.abik.server.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCityRequest(
        @NotNull(message = "ID региона обязателен")
        Long regionId,

        @NotBlank(message = "Название города обязательно")
        @Size(max = 150)
        String name
) {}