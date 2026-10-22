package info.prorabka.abik.server.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DeviceRegisterRequest(
        @NotBlank(message = "Токен обязателен")
        String token,

        @NotBlank(message = "Платформа обязательна")
        @Pattern(regexp = "ANDROID|IOS|WEB")
        String platform
) {}