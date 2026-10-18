package info.prorabka.abik.server.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRegionRequest(
        @NotBlank(message = "Название региона обязательно")
        @Size(max = 100)
        String name,

        @NotBlank(message = "ISO-код региона обязателен")
        @Size(max = 10)
        String code,

        @NotBlank(message = "Часовой пояс обязателен")
        @Size(max = 50)
        String timezone
) {}