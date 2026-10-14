package info.prorabka.abik.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Запрос на логин.
 */
public record LoginRequest(

        @NotBlank(message = "Телефон обязателен")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$")
        String phone,

        @NotBlank(message = "Пароль обязателен")
        String password
) {}