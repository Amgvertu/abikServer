package info.prorabka.abik.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Запрос на старт регистрации.
 */
public record RegisterStartRequest(

        @NotBlank(message = "Телефон обязателен")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$",
                message = "Телефон в формате E.164 или 10-15 цифр")
        String phone,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 100, message = "Пароль от 8 до 100 символов")
        String password,

        @NotNull(message = "Согласие на обработку ПД обязательно")
        Boolean acceptedPd,

        @NotBlank(message = "Версия политики обязательна")
        String policyVersion
) {}
